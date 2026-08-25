package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class Gecko449OracleFixtureRegressionTest {
  @Test
  void catmullRomFixtureRetainsOracleEasingSemantics() {
    var result = parse("LERP-002.json");

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frames = result.value().get(0).tracks().get(0).rotation().keyframes();
    assertEquals(InterpolationIR.CATMULLROM, frames.get(0).interpolationAfter());
    assertEquals(
        1.25,
        Gecko449EasingEvaluator.apply(frames.get(0).interpolationAfter(), List.of(), 0.25),
        1e-12);
  }

  @Test
  void constantMolangFixtureMatchesOracleValue() {
    var result = parse("MOLANG-002.json");

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frames = result.value().get(0).tracks().get(0).rotation().keyframes();
    assertEquals(new Vec3d(5, 4, 5), frames.get(0).incomingValue());
    assertEquals(new Vec3d(5, 4, 5), frames.get(1).incomingValue());
  }

  @Test
  void prePostFixtureMatchesGecko449PreWinsOracle() {
    var result = parse("PREPOST-004.json");

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frame = result.value().get(0).tracks().get(0).rotation().keyframes().get(0);
    assertEquals(new Vec3d(1, 2, 3), frame.incomingValue());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449)));
  }

  @Test
  void dynamicMolangFixtureIsRejectedByOfflinePolicy() {
    var result = parse("MOLANG-003.json");

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED,
        result.diagnostics().errors().get(0).code().value());
  }

  private static io.github.gabriel0liv.cpmconverter.diagnostics.Result<
          List<io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR>>
      parse(String fixture) {
    return new GeckoAnimationParser().parse(oracleFixture(fixture), geometry());
  }

  private static Path oracleFixture(String fixture) {
    return Path.of("..", "spikes", "geckolib-animation-semantics", "fixtures", fixture).normalize();
  }

  private static ModelIR geometry() {
    Path input = Path.of("..", "test-fixtures", "fixture-a-humanoid", "geometry.geo.json").normalize();
    var result = new GeckoGeometryParser().parse(input);
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    return result.value();
  }
}
