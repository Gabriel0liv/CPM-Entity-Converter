package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import org.junit.jupiter.api.Test;

/** Verifies that decoder boundaries return located Results rather than leaking input exceptions. */
final class IrBuilderTest {
  private static final SourceLocation LOCATION =
      SourceLocation.of(new SourcePath("fixture/geometry.geo.json"));

  @Test
  void boneBuilderRejectsBlankNamesAndKeepsProvenance() {
    var invalid = new BoneIrBuilder().build("body", "", LOCATION);
    assertFalse(invalid.success());
    assertTrue(invalid.diagnostics().hasErrors());
    var valid = new BoneIrBuilder().build("body", "body", LOCATION);
    assertTrue(valid.success());
    assertTrue(valid.value().provenance().contains("fixture/geometry.geo.json"));
  }

  @Test
  void clipBuilderValidatesDurationAndCustomIdentifier() {
    assertFalse(
        new AnimationClipIrBuilder().build("idle", 0, PlaybackMode.LOOP, null, LOCATION).success());
    assertFalse(
        new AnimationClipIrBuilder()
            .build("idle", 1, PlaybackMode.CUSTOM, null, LOCATION)
            .success());
    assertTrue(
        new AnimationClipIrBuilder().build("idle", 1, PlaybackMode.HOLD, null, LOCATION).success());
  }

  @Test
  void modelBuilderRejectsMissingBoundaryCollections() {
    var result = new ModelIrBuilder().build(null, "fixture", null, null, null, null, LOCATION);
    assertFalse(result.success());
    assertTrue(result.diagnostics().hasErrors());
  }
}
