package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import java.util.List;
import org.junit.jupiter.api.Test;

class Gecko449EasingEvaluatorTest {
  private static final double EPSILON = 1e-12;

  @Test
  void reproducesLinearStepAndSineOracleValues() {
    assertEquals(
        0.5, Gecko449EasingEvaluator.apply(InterpolationIR.LINEAR, List.of(), 0.5), EPSILON);
    assertEquals(0.0, Gecko449EasingEvaluator.apply(InterpolationIR.STEP, List.of(), 0.5), EPSILON);
    assertEquals(
        0.2928932188134524,
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_IN_SINE, List.of(), 0.5),
        EPSILON);
  }

  @Test
  void usesOnlyFirstEasingArgumentLikeGecko449() {
    double withExtraArg =
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_IN_BACK, List.of(1.2, 0.35), 0.5);
    double firstArgOnly =
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_IN_BACK, List.of(1.2), 0.5);

    assertEquals(-0.130237, withExtraArg, EPSILON);
    assertEquals(firstArgOnly, withExtraArg, EPSILON);
  }

  @Test
  void reproducesGecko449EaseInQuintRegistrationQuirk() {
    assertEquals(
        0.0625,
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_IN_QUINT, List.of(), 0.5),
        EPSILON);
  }

  @Test
  void reproducesElasticAndBounceFamilies() {
    assertEquals(
        1.1092540061122054,
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_IN_ELASTIC, List.of(1.2, 0.35), 0.5),
        EPSILON);
    // GeckoLib 4.4.9 computes 6f / 11f in float before promoting to double.
    assertEquals(
        0.4687499776482542,
        Gecko449EasingEvaluator.apply(InterpolationIR.EASE_OUT_BOUNCE, List.of(), 0.5),
        EPSILON);
  }

  @Test
  void reproducesCatmullRomRegistrationSemantics() {
    assertEquals(
        1.25, Gecko449EasingEvaluator.apply(InterpolationIR.CATMULLROM, List.of(), 0.25), EPSILON);
    assertEquals(
        -0.5, Gecko449EasingEvaluator.apply(InterpolationIR.CATMULLROM, List.of(), 0.5), EPSILON);
  }
}
