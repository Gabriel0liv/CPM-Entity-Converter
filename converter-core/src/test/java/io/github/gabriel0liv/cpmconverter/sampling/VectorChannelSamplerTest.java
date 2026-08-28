package io.github.gabriel0liv.cpmconverter.sampling;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import org.junit.jupiter.api.Test;

class VectorChannelSamplerTest {
  private static final InterpolationEvaluator TEST_EVALUATOR =
      (kind, args, t) ->
          switch (kind) {
            case LINEAR -> t;
            case STEP -> t < 0.75 ? 0.0 : 0.5;
            default -> 0.25;
          };

  @Test
  void missingChannelReturnsProvidedIdentity() {
    var identity = new Vec3d(1, 1, 1);

    var result = VectorChannelSampler.sample(null, 0.5, identity, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(identity, result.value());
  }

  @Test
  void linearChannelInterpolatesOutgoingToNextIncoming() {
    var channel =
        channel(
            key(0.0, new Vec3d(0, 0, 0), InterpolationIR.LINEAR),
            key(1.0, new Vec3d(10, 20, 30), InterpolationIR.LINEAR));

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(5, 10, 15), result.value());
  }

  @Test
  void exactInteriorKeyframeTimeUsesThatKeyframesValue() {
    var channel =
        channel(
            key(0.0, new Vec3d(0, 0, 0), InterpolationIR.LINEAR),
            key(1.0, new Vec3d(10, 20, 30), InterpolationIR.LINEAR),
            key(2.0, new Vec3d(20, 40, 60), InterpolationIR.LINEAR));

    var result = VectorChannelSampler.sample(channel, 1.0, Vec3d.ZERO, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(10, 20, 30), result.value());
  }

  @Test
  void beforeFirstTimestampHoldsFirstValueLikeGecko449() {
    var channel =
        channel(
            key(0.5, new Vec3d(4, 5, 6), InterpolationIR.LINEAR),
            key(1.0, new Vec3d(10, 20, 30), InterpolationIR.LINEAR));

    var result = VectorChannelSampler.sample(channel, 0.25, Vec3d.ZERO, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(4, 5, 6), result.value());
  }

  @Test
  void afterLastTimestampHoldsLastValueLikeGecko449() {
    var channel =
        channel(
            key(0.0, new Vec3d(0, 0, 0), InterpolationIR.LINEAR),
            key(1.0, new Vec3d(10, 20, 30), InterpolationIR.LINEAR));

    var result = VectorChannelSampler.sample(channel, 2.0, Vec3d.ZERO, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(10, 20, 30), result.value());
  }

  @Test
  void easingProgressIsAppliedToWholeVectorDifference() {
    var channel =
        channel(
            key(0.0, new Vec3d(0, 0, 0), InterpolationIR.EASE_IN_SINE),
            key(1.0, new Vec3d(8, 12, 20), InterpolationIR.LINEAR));

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, TEST_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(2, 3, 5), result.value());
  }

  @Test
  void easingArgumentsAreForwardedWithoutModification() {
    var args = List.of(1.2, 0.35);
    var channel =
        new ChannelIR<>(
            "position",
            TransformMode.ADDITIVE,
            TransformSpace.LOCAL,
            List.of(
                new KeyframeIR<>(
                    0.0,
                    Vec3d.ZERO,
                    Vec3d.ZERO,
                    InterpolationIR.EASE_IN_BACK,
                    args),
                key(1.0, new Vec3d(4, 4, 4), InterpolationIR.LINEAR)));
    var seen = new Object() {
      List<Double> value;
    };
    InterpolationEvaluator evaluator =
        (kind, actualArgs, t) -> {
          seen.value = actualArgs;
          return 0.5;
        };

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, evaluator);

    assertTrue(result.success());
    assertEquals(args, seen.value);
    assertEquals(new Vec3d(2, 2, 2), result.value());
  }

  @Test
  void evaluatorOvershootIsNotClamped() {
    var channel =
        channel(
            key(0.0, Vec3d.ZERO, InterpolationIR.EASE_OUT_BACK),
            key(1.0, new Vec3d(10, 0, 0), InterpolationIR.LINEAR));
    InterpolationEvaluator overshoot = (kind, args, t) -> 1.2;

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, overshoot);

    assertTrue(result.success());
    assertEquals(new Vec3d(12, 0, 0), result.value());
  }

  @Test
  void nonFiniteEvaluatorOutputReturnsStableDiagnostic() {
    var channel =
        channel(
            key(0.0, Vec3d.ZERO, InterpolationIR.LINEAR),
            key(1.0, new Vec3d(10, 0, 0), InterpolationIR.LINEAR));
    InterpolationEvaluator invalid = (kind, args, t) -> Double.NaN;

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, invalid);

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_SAMPLING_NON_FINITE,
        result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void emptyChannelReturnsStableDiagnostic() {
    var channel =
        new ChannelIR<Vec3d>(
            "position", TransformMode.ADDITIVE, TransformSpace.LOCAL, List.of());

    var result = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, TEST_EVALUATOR);

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
        result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void invalidSamplingArgumentsReturnRequestDiagnostic() {
    var channel =
        channel(
            key(0.0, Vec3d.ZERO, InterpolationIR.LINEAR),
            key(1.0, new Vec3d(1, 1, 1), InterpolationIR.LINEAR));

    var badTime = VectorChannelSampler.sample(channel, Double.NaN, Vec3d.ZERO, TEST_EVALUATOR);
    var badIdentity = VectorChannelSampler.sample(channel, 0.5, null, TEST_EVALUATOR);
    var badEvaluator = VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, null);

    for (var result : List.of(badTime, badIdentity, badEvaluator)) {
      assertFalse(result.success());
      assertEquals(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
          result.diagnostics().errors().get(0).code().value());
    }
  }

  private static ChannelIR<Vec3d> channel(KeyframeIR<Vec3d>... keyframes) {
    return new ChannelIR<>(
        "position", TransformMode.ADDITIVE, TransformSpace.LOCAL, List.of(keyframes));
  }

  private static KeyframeIR<Vec3d> key(double time, Vec3d value, InterpolationIR interpolation) {
    return new KeyframeIR<>(time, value, value, interpolation, List.of());
  }
}
