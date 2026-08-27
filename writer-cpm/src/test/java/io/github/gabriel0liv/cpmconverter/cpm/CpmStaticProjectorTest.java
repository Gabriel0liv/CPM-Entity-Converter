package io.github.gabriel0liv.cpmconverter.cpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceDescriptor;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class CpmStaticProjectorTest {
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  @Test
  void createsSixVanillaRootsAndKeepsAllGeckoBonesBelowBodyEntityRoot() {
    BoneId bodyId = new BoneId("body");
    BoneId headId = new BoneId("head");
    BoneId jawId = new BoneId("jaw");
    BoneIR body = bone(bodyId, null, List.of(headId), new Vec3d(0, -24, 0), List.of(), true);
    BoneIR head = bone(headId, bodyId, List.of(jawId), new Vec3d(0, 8, 0), List.of(), true);
    BoneIR jaw = bone(jawId, headId, List.of(), new Vec3d(0, 2, 0), List.of(), true);

    var result =
        new CpmStaticProjector()
            .project(model(List.of(body, head, jaw), List.of(bodyId)), settings(1, 0));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmStaticProjectV1 project = result.value();
    assertEquals(
        List.of(
            CpmVanillaPart.HEAD,
            CpmVanillaPart.BODY,
            CpmVanillaPart.LEFT_ARM,
            CpmVanillaPart.RIGHT_ARM,
            CpmVanillaPart.LEFT_LEG,
            CpmVanillaPart.RIGHT_LEG),
        project.roots().stream().map(CpmRootV1::vanillaPart).toList());

    for (CpmRootV1 root : project.roots()) {
      if (root.vanillaPart() == CpmVanillaPart.BODY) {
        assertEquals(1, root.children().size());
      } else {
        assertTrue(root.children().isEmpty());
      }
      assertFalse(root.showVanillaGeometry());
      assertFalse(root.disableVanillaAnim());
    }

    CpmElementV1 entityRoot = root(project, CpmVanillaPart.BODY).children().get(0);
    assertEquals(CpmElementKind.ENTITY_ROOT, entityRoot.kind());
    assertEquals(ProjectionKey.entityRoot(), entityRoot.key());
    assertVec(new Vec3d(0, 24, 0), entityRoot.transform().translation());
    assertVec(ONE, entityRoot.transform().scale());

    CpmElementV1 bodyNode = entityRoot.children().get(0);
    CpmElementV1 headNode = bodyNode.children().get(0);
    CpmElementV1 jawNode = headNode.children().get(0);
    assertEquals(ProjectionKey.bone(bodyId), bodyNode.key());
    assertEquals(ProjectionKey.bone(headId), headNode.key());
    assertEquals(ProjectionKey.bone(jawId), jawNode.key());
    assertEquals(bodyNode, project.logicalTargets().get(ProjectionKey.bone(bodyId)));
    assertEquals(jawNode, project.logicalTargets().get(ProjectionKey.bone(jawId)));
  }

  @Test
  void keepsModelScaleAndVerticalOffsetOnEntityRootOnly() {
    BoneId rootId = new BoneId("root");
    BoneIR sourceRoot = bone(rootId, null, List.of(), new Vec3d(1, -24, 3), List.of(), true);
    ModelIR model = model(List.of(sourceRoot), List.of(rootId));

    var result = new CpmStaticProjector().project(model, settings(2.5, 7));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmElementV1 entityRoot = root(result.value(), CpmVanillaPart.BODY).children().get(0);
    CpmElementV1 sourceNode = entityRoot.children().get(0);
    assertVec(new Vec3d(0, 31, 0), entityRoot.transform().translation());
    assertVec(new Vec3d(2.5, 2.5, 2.5), entityRoot.transform().scale());
    assertVec(new Vec3d(1, -24, 3), sourceNode.transform().translation());
    assertVec(ONE, sourceNode.transform().scale());
  }

  @Test
  void rejectsScaleBelowCpmExactFloorAndNonFiniteSettings() {
    ModelIR model = model(List.of(), List.of());

    var floor = new CpmStaticProjector().project(model, settings(0.01, 0));
    var below = new CpmStaticProjector().project(model, settings(0.009, 0));
    var nonFinite = new CpmStaticProjector().project(model, settings(1, Double.NaN));

    assertTrue(floor.success(), () -> floor.diagnostics().all().toString());
    assertFalse(below.success());
    assertHasCode(below, DiagnosticCodes.CPM_PROJECTION_MODEL_SCALE);
    assertFalse(nonFinite.success());
    assertHasCode(nonFinite, DiagnosticCodes.CPM_PROJECTION_INVALID_SETTING);
  }

  @Test
  void usesHelperOnlyForPivotedOrRotatedCubeAndPreservesCpmFacingFields() {
    BoneId boneId = new BoneId("body");
    CubeIR direct =
        cube(
            "direct",
            boneId,
            new Vec3d(1.5, 2.25, -3),
            new Vec3d(2, 3, 4),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0.25,
            true,
            new BoxUvIR(1.5, 2.25));
    CubeIR rotated =
        cube(
            "rotated",
            boneId,
            new Vec3d(-1, -2, -3),
            new Vec3d(4, 5, 6),
            new Vec3d(3, 4, 5),
            Quatd.fromEulerZYX(Math.toRadians(10), Math.toRadians(20), Math.toRadians(30)),
            0.5,
            false,
            new BoxUvIR(7, 9));
    BoneIR body = bone(boneId, null, List.of(), Vec3d.ZERO, List.of(direct, rotated), true);

    var result =
        new CpmStaticProjector().project(model(List.of(body), List.of(boneId)), settings(1, 0));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmElementV1 boneNode = result.value().logicalTargets().get(ProjectionKey.bone(boneId));
    assertEquals(2, boneNode.children().size());

    CpmElementV1 directNode = boneNode.children().get(0);
    assertEquals(CpmElementKind.CUBE, directNode.kind());
    assertEquals(ProjectionKey.cube(direct.id()), directNode.key());
    assertVec(Vec3d.ZERO, directNode.transform().translation());
    assertVec(Vec3d.ZERO, directNode.transform().rotationDegrees());
    assertVec(new Vec3d(1.5, 2.25, -3), directNode.offset());
    assertVec(new Vec3d(2, 3, 4), directNode.size());
    assertEquals(0.25, directNode.mcScale(), 1e-12);
    assertTrue(directNode.mirror());
    assertEquals(direct.uv(), directNode.uv());

    CpmElementV1 helper = boneNode.children().get(1);
    assertEquals(CpmElementKind.CUBE_HELPER, helper.kind());
    assertEquals(ProjectionKey.cubeHelper(rotated.id()), helper.key());
    assertVec(new Vec3d(3, 4, 5), helper.transform().translation());
    assertVec(new Vec3d(10, 20, 30), helper.transform().rotationDegrees());
    assertStructural(helper);
    assertEquals(1, helper.children().size());

    CpmElementV1 rotatedNode = helper.children().get(0);
    assertEquals(CpmElementKind.CUBE, rotatedNode.kind());
    assertVec(Vec3d.ZERO, rotatedNode.transform().translation());
    assertVec(Vec3d.ZERO, rotatedNode.transform().rotationDegrees());
    assertVec(new Vec3d(-1, -2, -3), rotatedNode.offset());
    assertEquals(rotated.uv(), rotatedNode.uv());
  }

  @Test
  void preservesSignedFractionalPerFaceUvWithoutQuantization() {
    BoneId boneId = new BoneId("body");
    LinkedHashMap<String, FaceUvIR> faces = new LinkedHashMap<>();
    faces.put("north", new FaceUvIR(1.25, 2.5, -3.75, 4.5));
    faces.put("up", new FaceUvIR(5.125, 6.25, 7.5, -8.75));
    PerFaceUvIR uv = new PerFaceUvIR(faces);
    CubeIR cube =
        cube(
            "fractional",
            boneId,
            Vec3d.ZERO,
            new Vec3d(1, 1, 1),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            uv);
    BoneIR body = bone(boneId, null, List.of(), Vec3d.ZERO, List.of(cube), true);

    var result =
        new CpmStaticProjector().project(model(List.of(body), List.of(boneId)), settings(1, 0));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmElementV1 cubeNode = result.value().logicalTargets().get(ProjectionKey.cube(cube.id()));
    assertEquals(uv, cubeNode.uv());
    PerFaceUvIR projected = (PerFaceUvIR) cubeNode.uv();
    assertEquals(-3.75, projected.faces().get("north").width(), 0);
    assertEquals(-8.75, projected.faces().get("up").height(), 0);
  }

  @Test
  void neverRenderHidesOnlyOwnCubeGeometryAndKeepsDescendantsActive() {
    BoneId parentId = new BoneId("hidden-own-cubes");
    BoneId childId = new BoneId("child");
    CubeIR parentCube =
        cube(
            "parent-cube",
            parentId,
            Vec3d.ZERO,
            ONE,
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            new BoxUvIR(0, 0));
    CubeIR childCube =
        cube(
            "child-cube",
            childId,
            Vec3d.ZERO,
            ONE,
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            new BoxUvIR(0, 0));
    BoneIR parent = bone(parentId, null, List.of(childId), Vec3d.ZERO, List.of(parentCube), false);
    BoneIR child = bone(childId, parentId, List.of(), Vec3d.ZERO, List.of(childCube), true);

    var result =
        new CpmStaticProjector()
            .project(model(List.of(parent, child), List.of(parentId)), settings(1, 0));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmElementV1 parentNode = result.value().logicalTargets().get(ProjectionKey.bone(parentId));
    CpmElementV1 parentCubeNode =
        result.value().logicalTargets().get(ProjectionKey.cube(parentCube.id()));
    CpmElementV1 childNode = result.value().logicalTargets().get(ProjectionKey.bone(childId));
    CpmElementV1 childCubeNode =
        result.value().logicalTargets().get(ProjectionKey.cube(childCube.id()));

    assertStructural(parentNode);
    assertTrue(parentCubeNode.hidden());
    assertStructural(childNode);
    assertFalse(childCubeNode.hidden());
  }

  @Test
  void projectionIsDeterministicAndLogicalTargetIterationFollowsPreorder() {
    BoneId rootId = new BoneId("root");
    CubeIR firstCube =
        cube("a", rootId, Vec3d.ZERO, ONE, Vec3d.ZERO, Quatd.IDENTITY, 0, false, new BoxUvIR(0, 0));
    CubeIR secondCube =
        cube(
            "b",
            rootId,
            Vec3d.ZERO,
            ONE,
            new Vec3d(1, 0, 0),
            Quatd.IDENTITY,
            0,
            false,
            new BoxUvIR(1, 0));
    BoneIR root = bone(rootId, null, List.of(), Vec3d.ZERO, List.of(firstCube, secondCube), true);
    ModelIR model = model(List.of(root), List.of(rootId));
    CpmProjectionSettings settings = settings(1, 0);

    var first = new CpmStaticProjector().project(model, settings);
    var second = new CpmStaticProjector().project(model, settings);

    assertTrue(first.success(), () -> first.diagnostics().all().toString());
    assertTrue(second.success(), () -> second.diagnostics().all().toString());
    assertEquals(first.value(), second.value());
    assertEquals(
        List.of(
            ProjectionKey.entityRoot(),
            ProjectionKey.bone(rootId),
            ProjectionKey.cube(firstCube.id()),
            ProjectionKey.cubeHelper(secondCube.id()),
            ProjectionKey.cube(secondCube.id())),
        first.value().logicalTargets().keySet().stream().toList());
  }

  private static CpmProjectionSettings settings(double scale, double offset) {
    return new CpmProjectionSettings(scale, offset, true, false);
  }

  private static ModelIR model(List<BoneIR> bones, List<BoneId> roots) {
    return new ModelIR(
        new SourceDescriptor("fixture.geo.json", "1.12.0"),
        new GeometryId("fixture:model"),
        bones,
        roots,
        List.of(),
        List.of(),
        List.of());
  }

  private static BoneIR bone(
      BoneId id,
      BoneId parent,
      List<BoneId> children,
      Vec3d translation,
      List<CubeIR> cubes,
      boolean renderOwnCubes) {
    return new BoneIR(
        id,
        id.value(),
        parent,
        children,
        new Transform(translation, Quatd.IDENTITY, ONE),
        cubes,
        renderOwnCubes,
        "fixture#" + id.value());
  }

  private static CubeIR cube(
      String id,
      BoneId bone,
      Vec3d origin,
      Vec3d size,
      Vec3d pivot,
      Quatd rotation,
      double inflate,
      boolean mirror,
      io.github.gabriel0liv.cpmconverter.ir.UvIR uv) {
    return new CubeIR(
        new CubeId(id), bone, origin, size, pivot, rotation, inflate, mirror, uv, "fixture#" + id);
  }

  private static CpmRootV1 root(CpmStaticProjectV1 project, CpmVanillaPart part) {
    return project.roots().stream()
        .filter(root -> root.vanillaPart() == part)
        .findFirst()
        .orElseThrow();
  }

  private static void assertStructural(CpmElementV1 element) {
    assertTrue(element.show());
    assertFalse(element.hidden());
    assertFalse(element.texture());
    assertVec(Vec3d.ZERO, element.offset());
    assertVec(Vec3d.ZERO, element.size());
  }

  private static void assertVec(Vec3d expected, Vec3d actual) {
    assertNotNull(actual);
    assertEquals(expected.x(), actual.x(), 1e-9);
    assertEquals(expected.y(), actual.y(), 1e-9);
    assertEquals(expected.z(), actual.z(), 1e-9);
  }

  private static void assertHasCode(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result, String code) {
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> "missing diagnostic " + code + " in " + result.diagnostics().all());
  }
}
