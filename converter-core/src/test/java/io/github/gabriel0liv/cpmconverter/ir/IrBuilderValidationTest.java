package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import org.junit.jupiter.api.Test;

final class IrBuilderValidationTest {
  private static final SourceLocation LOCATION =
      SourceLocation.of(new SourcePath("fixtures/a.geo.json"));

  @Test
  void buildersReturnLocatedFailuresForInvalidInput() {
    var result = new BoneIrBuilder().build("", "body", LOCATION);
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().allMatch(d -> LOCATION.equals(d.location())));
  }

  @Test
  void clipBuilderRejectsInvalidDurationWithoutThrowing() {
    var result =
        new AnimationClipIrBuilder().build("idle", Double.NaN, PlaybackMode.LOOP, null, LOCATION);
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().get(0).location().equals(LOCATION));
  }
}
