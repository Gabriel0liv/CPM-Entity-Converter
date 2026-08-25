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
        new ModelIrValidator().validate(model).errors().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_CUBE_BONE_MISMATCH)));
  }
}
