package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelIndexTest {
  private static ModelIR model(BoneIR... bones) {
    return new ModelIR(
        new SourceDescriptor("fixture.geo.json", "geometry"),
        new GeometryId("g"),
        List.of(bones),
        List.of(bones[0].id()),
        List.of(
            new AnimationClipIR(
                new ClipId("idle"),
                1.0,
                PlaybackMode.LOOP,
                null,
                List.of(),
                List.of(),
                SourceLocation.of(new SourcePath("fixture.animation.json")))),
        List.of(),
        List.of());
  }

  @Test
  void resolvesUniqueBoneAndClip() {
    BoneIR head =
        new BoneIR(
            new BoneId("head-id"),
            "head",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    var index = new ModelIndex(model(head));
    assertEquals(new BoneId("head-id"), index.bone("head").value());
    assertEquals(new ClipId("idle"), index.clip("idle").value());
  }

  @Test
  void reportsAmbiguousBoneWithDeterministicCandidates() {
    BoneIR first =
        new BoneIR(
            new BoneId("first"),
            "joint",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    BoneIR second =
        new BoneIR(
            new BoneId("second"),
            "joint",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    var result = new ModelIndex(model(first, second)).bone("joint");
    assertFalse(result.success());
    assertEquals("CONFIG_BONE_AMBIGUOUS", result.diagnostics().errors().get(0).code().value());
    assertEquals("first", result.diagnostics().errors().get(0).context().get("candidate.0"));
    assertEquals("second", result.diagnostics().errors().get(0).context().get("candidate.1"));
  }

  @Test
  void reportsMissingReferences() {
    BoneIR head =
        new BoneIR(
            new BoneId("head-id"),
            "head",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    var index = new ModelIndex(model(head));
    assertFalse(index.bone("missing").success());
    assertFalse(index.clip("missing").success());
  }
}
