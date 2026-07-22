package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParsedGeometryValidatorTest {
  private static final SourceLocation SOURCE =
      new SourceLocation(
          new SourcePath("fixture.geo.json"), null, null, "/minecraft:geometry/0/bones/0", null);

  @Test
  void acceptsValidBoundary() {
    assertTrue(new ParsedGeometryValidator().validate(valid()).success());
  }

  @Test
  void rejectsDuplicateBoneIdAndName() {
    ParsedGeometry base = valid();
    ParsedBone first = base.bones().get(0);
    ParsedBone duplicate =
        new ParsedBone(
            first.id(),
            first.sourceName(),
            null,
            List.of(),
            first.bindLocal(),
            0,
            false,
            List.of(),
            SOURCE);
    ParsedGeometry invalid =
        new ParsedGeometry(
            base.source(),
            base.geometryId(),
            32,
            32,
            List.of(first, duplicate),
            List.of(first.id()),
            List.of());
    var result = new ParsedGeometryValidator().validate(invalid);
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_DUPLICATE_BONE_ID)));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.GEO_DUPLICATE_BONE_NAME)));
  }

  @Test
  void rejectsCycleAndUnreachableBone() {
    ParsedGeometry base = valid();
    ParsedBone root = base.bones().get(0);
    ParsedBone child = base.bones().get(1);
    ParsedBone cyclicRoot =
        new ParsedBone(
            root.id(),
            root.sourceName(),
            child.id(),
            List.of(child.id()),
            root.bindLocal(),
            0,
            false,
            root.cubes(),
            root.source());
    ParsedBone cyclicChild =
        new ParsedBone(
            child.id(),
            child.sourceName(),
            root.id(),
            List.of(),
            child.bindLocal(),
            0,
            false,
            child.cubes(),
            child.source());
    var result =
        new ParsedGeometryValidator()
            .validate(
                new ParsedGeometry(
                    base.source(),
                    base.geometryId(),
                    32,
                    32,
                    List.of(cyclicRoot, cyclicChild),
                    List.of(root.id()),
                    List.of()));
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.GEO_HIERARCHY_CYCLE)));
  }

  @Test
  void rejectsCubeOwnershipAndNegativeSize() {
    ParsedGeometry base = valid();
    ParsedCube cube = base.bones().get(0).cubes().get(0);
    ParsedCube invalidCube =
        new ParsedCube(
            new CubeId("duplicate"),
            new BoneId("missing"),
            cube.origin(),
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(-1, 1, 1),
            cube.pivot(),
            cube.rotationDegrees(),
            cube.inflate(),
            cube.mirror(),
            cube.rawUv(),
            cube.source());
    ParsedBone bone =
        new ParsedBone(
            base.bones().get(0).id(),
            "body",
            null,
            List.of(),
            Transform.identity(),
            0,
            false,
            List.of(invalidCube),
            SOURCE);
    var result =
        new ParsedGeometryValidator()
            .validate(
                new ParsedGeometry(
                    base.source(),
                    base.geometryId(),
                    32,
                    32,
                    List.of(bone),
                    List.of(bone.id()),
                    List.of()));
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_CUBE_BONE_MISSING)));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_INVALID_VALUE)));
  }

  @Test
  void validatorCanBeReusedConcurrently() throws Exception {
    ParsedGeometry geometry = valid();
    ParsedGeometryValidator validator = new ParsedGeometryValidator();
    var first = validator.validate(geometry);
    var second = validator.validate(geometry);
    assertTrue(first.success() && second.success());
    var thread = new Thread(() -> assertTrue(validator.validate(geometry).success()));
    thread.start();
    thread.join();
  }

  @Test
  void reportsParentOmittingChildAndUnreachableFromRoots() {
    ParsedGeometry base = valid();
    ParsedBone root = base.bones().get(0);
    ParsedBone child = base.bones().get(1);
    ParsedBone omitted =
        new ParsedBone(
            root.id(),
            root.sourceName(),
            null,
            List.of(),
            root.bindLocal(),
            0,
            false,
            root.cubes(),
            SOURCE);
    var omittedResult =
        new ParsedGeometryValidator()
            .validate(
                new ParsedGeometry(
                    base.source(),
                    base.geometryId(),
                    32,
                    32,
                    List.of(omitted, child),
                    List.of(root.id()),
                    List.of()));
    assertTrue(
        omittedResult.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_UNREACHABLE_BONE)));
    assertTrue(
        omittedResult.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_PARENT_CHILD_MISMATCH)));
  }

  @Test
  void reportsMissingBoneAndCubeProvenance() {
    ParsedGeometry base = valid();
    ParsedBone root = base.bones().get(0);
    ParsedCube cube = root.cubes().get(0);
    ParsedCube missingSourceCube =
        new ParsedCube(
            cube.id(),
            root.id(),
            cube.origin(),
            cube.size(),
            cube.pivot(),
            cube.rotationDegrees(),
            cube.inflate(),
            cube.mirror(),
            cube.rawUv(),
            null);
    ParsedBone missingSourceBone =
        new ParsedBone(
            root.id(),
            root.sourceName(),
            null,
            root.children(),
            root.bindLocal(),
            root.inflate(),
            root.mirror(),
            List.of(missingSourceCube),
            null);
    var result =
        new ParsedGeometryValidator()
            .validate(
                new ParsedGeometry(
                    base.source(),
                    base.geometryId(),
                    32,
                    32,
                    List.of(missingSourceBone, base.bones().get(1)),
                    base.roots(),
                    List.of()));
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_INVALID_VALUE)));
  }

  private static ParsedGeometry valid() {
    BoneId rootId = new BoneId("g/bone/0");
    BoneId childId = new BoneId("g/bone/1");
    ParsedCube cube =
        new ParsedCube(
            new CubeId("g/bone/0/cube/0"),
            rootId,
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(0, 0, 0),
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(1, 1, 1),
            io.github.gabriel0liv.cpmconverter.math.Vec3d.ZERO,
            io.github.gabriel0liv.cpmconverter.math.Vec3d.ZERO,
            0,
            false,
            null,
            SOURCE);
    ParsedBone root =
        new ParsedBone(
            rootId,
            "body",
            null,
            List.of(childId),
            Transform.identity(),
            0,
            false,
            List.of(cube),
            SOURCE);
    ParsedBone child =
        new ParsedBone(
            childId, "head", rootId, List.of(), Transform.identity(), 0, false, List.of(), SOURCE);
    return new ParsedGeometry(
        new SourcePath("fixture.geo.json"),
        new GeometryId("g"),
        32,
        32,
        List.of(root, child),
        List.of(rootId),
        List.of());
  }
}
