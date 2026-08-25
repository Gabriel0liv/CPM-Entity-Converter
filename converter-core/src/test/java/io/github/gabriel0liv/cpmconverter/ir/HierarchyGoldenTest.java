package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import org.junit.jupiter.api.Test;

class HierarchyGoldenTest {
  @Test
  void sourceOrderAndDefensiveCopies() {
    BoneIR body =
        new BoneIR(
            new BoneId("body"),
            "body",
            null,
            List.of(new BoneId("neck")),
            Transform.identity(),
            List.of(),
            "fixture");
    BoneIR neck =
        new BoneIR(
            new BoneId("neck"),
            "neck",
            new BoneId("body"),
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    ModelIR model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "1.12.0"),
            List.of(body, neck),
            List.of(new BoneId("body")),
            List.of(),
            List.of());
    assertEquals("body", model.bones().get(0).name());
    assertFalse(new ModelIrValidator().validate(model).hasErrors());
  }

  @Test
  void validationPreservesCubeSourceOrderOwnershipAndProvenance() {
    CubeIR secondInNameOrder = cube("cube-z", "body", "source:cube-z");
    CubeIR firstInNameOrder = cube("cube-a", "body", "source:cube-a");
    BoneIR body =
        new BoneIR(
            new BoneId("body"),
            "body",
            null,
            List.of(),
            Transform.identity(),
            List.of(secondInNameOrder, firstInNameOrder),
            "source:body");
    ModelIR model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "1.12.0"),
            List.of(body),
            List.of(body.id()),
            List.of(),
            List.of());

    assertFalse(new ModelIrValidator().validate(model).hasErrors());
    assertEquals(List.of("cube-z", "cube-a"), body.cubes().stream().map(cube -> cube.id().value()).toList());
    assertEquals(List.of("source:cube-z", "source:cube-a"), body.cubes().stream().map(CubeIR::provenance).toList());
    assertTrue(body.cubes().stream().allMatch(cube -> cube.bone().equals(body.id())));
  }

  @Test
  void rejectsCubeWhoseDeclaredBoneDiffersFromContainingBone() {
    CubeIR misplaced =
        new CubeIR(
            new CubeId("cube"),
            new BoneId("neck"),
            Vec3d.ZERO,
            new Vec3d(1, 1, 1),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            new BoxUvIR(0, 0),
            "fixture");
    BoneIR body =
        new BoneIR(
            new BoneId("body"),
            "body",
            null,
            List.of(new BoneId("neck")),
            Transform.identity(),
            List.of(misplaced),
            "fixture");
    BoneIR neck =
        new BoneIR(
            new BoneId("neck"),
            "neck",
            new BoneId("body"),
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    ModelIR model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "1.12.0"),
            List.of(body, neck),
            List.of(new BoneId("body")),
            List.of(),
            List.of());

    assertTrue(
        new ModelIrValidator()
            .validate(model).errors().stream()
                .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_CUBE_BONE_MISMATCH)));
  }

  private static CubeIR cube(String id, String bone, String provenance) {
    return new CubeIR(
        new CubeId(id),
        new BoneId(bone),
        Vec3d.ZERO,
        new Vec3d(1, 1, 1),
        Vec3d.ZERO,
        Quatd.IDENTITY,
        0,
        false,
        new BoxUvIR(0, 0),
        provenance);
  }
}
