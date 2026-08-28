package io.github.gabriel0liv.cpmconverter.sampling;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import io.github.gabriel0liv.cpmconverter.ir.RotationOrder;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationKeyframeIR;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.math.Vec3i;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RotationChannelSamplerTest {
  private static final InterpolationEvaluator LINEAR_EVALUATOR =
      (interpolation, args, progress) -> progress;

  @Test
  void missingRotationUsesIdentityAndNeutralContinuity() {
    var result = RotationChannelSampler.sample(null, 0.5, LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(Quatd.IDENTITY, result.value().rotation());
    assertEquals(Vec3d.ZERO, result.value().continuity().sourceEulerHint());
    assertEquals(new Vec3i(0, 0, 0), result.value().continuity().winding());
    assertEquals(Optional.empty(), result.value().continuity().previousOutputEuler());
  }

  @Test
  void preservesAuthoredTwoTurnWinding() {
    var channel = rotations(List.of(key(0.0, 0, 0, 0), key(1.0, 360, 0, 0), key(2.0, 720, 0, 0)));

    var result = RotationChannelSampler.sample(channel, 1.5, LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(540.0, result.value().continuity().sourceEulerHint().x(), 1e-12);
    assertEquals(1, result.value().continuity().winding().x());
    assertEquals(Optional.empty(), result.value().continuity().previousOutputEuler());
  }

  @Test
  void exactNegativeFullTurnKeepsNegativeWinding() {
    var channel = rotations(List.of(key(0.0, 0, 0, 0), key(1.0, -360, 0, 0)));

    var result = RotationChannelSampler.sample(channel, 1.0, LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(-360.0, result.value().continuity().sourceEulerHint().x(), 0.0);
    assertEquals(-1, result.value().continuity().winding().x());
  }

  @Test
  void doesNotRewriteAuthored179ToMinus179AsShortestPath() {
    var channel = rotations(List.of(key(0.0, 179, 0, 0), key(1.0, -179, 0, 0)));

    var result = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(0.0, result.value().continuity().sourceEulerHint().x(), 1e-12);
    assertEquals(0, result.value().continuity().winding().x());
  }

  @Test
  void convertsInstantaneousEulerUsingZyxQuaternion() {
    var channel = rotations(List.of(key(0.0, 10, 20, 30), key(1.0, 50, 60, 70)));

    var result = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR);
    var expected = Quatd.fromEulerZYX(Math.toRadians(30), Math.toRadians(40), Math.toRadians(50));

    assertTrue(result.success());
    assertQuatEquivalent(expected, result.value().rotation());
    assertEquals(new Vec3d(30, 40, 50), result.value().continuity().sourceEulerHint());
  }

  @Test
  void usesOutgoingLeftAndIncomingRightValuesForSegment() {
    var left =
        new SourceRotationKeyframeIR(
            0.0, Vec3d.ZERO, new Vec3d(20, 40, 60), InterpolationIR.LINEAR, List.of(), "left");
    var right =
        new SourceRotationKeyframeIR(
            1.0,
            new Vec3d(40, 80, 120),
            new Vec3d(99, 99, 99),
            InterpolationIR.LINEAR,
            List.of(),
            "right");
    var channel = rotations(List.of(left, right));

    var result = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(new Vec3d(30, 60, 90), result.value().continuity().sourceEulerHint());
  }

  @Test
  void easingProgressIsAppliedToContinuousEulerComponentsWithoutClamping() {
    var channel = rotations(List.of(key(0.0, 0, 0, 0), key(1.0, 100, 200, 300)));
    InterpolationEvaluator overshoot = (interpolation, args, progress) -> 1.2;

    var result = RotationChannelSampler.sample(channel, 0.5, overshoot);

    assertTrue(result.success());
    assertEquals(new Vec3d(120, 240, 360), result.value().continuity().sourceEulerHint());
  }

  @Test
  void beforeAndAfterChannelHoldAuthoredEndpointValues() {
    var channel = rotations(List.of(key(0.5, 45, 90, 135), key(1.0, 90, 180, 270)));

    var before = RotationChannelSampler.sample(channel, 0.25, LINEAR_EVALUATOR);
    var after = RotationChannelSampler.sample(channel, 2.0, LINEAR_EVALUATOR);

    assertEquals(new Vec3d(45, 90, 135), before.value().continuity().sourceEulerHint());
    assertEquals(new Vec3d(90, 180, 270), after.value().continuity().sourceEulerHint());
  }

  @Test
  void sourceKeyframesAreNotMutated() {
    var first = key(0.0, 0, 0, 0);
    var second = key(1.0, 360, 90, -360);
    var channel = rotations(List.of(first, second));

    RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR);

    assertEquals(Vec3d.ZERO, first.incomingValue());
    assertEquals(new Vec3d(360, 90, -360), second.incomingValue());
  }

  @Test
  void emptyChannelReturnsStableDiagnostic() {
    var channel = new SourceRotationChannelIR(List.of(), RotationOrder.ZYX);

    var result = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR);

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
        result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void nonFiniteEvaluatorOutputReturnsStableDiagnostic() {
    var channel = rotations(List.of(key(0.0, 0, 0, 0), key(1.0, 90, 0, 0)));
    InterpolationEvaluator invalid = (interpolation, args, progress) -> Double.NaN;

    var result = RotationChannelSampler.sample(channel, 0.5, invalid);

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_SAMPLING_NON_FINITE,
        result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void invalidSamplingArgumentsReturnRequestDiagnostic() {
    var channel = rotations(List.of(key(0.0, 0, 0, 0), key(1.0, 90, 0, 0)));

    var badTime = RotationChannelSampler.sample(channel, Double.NaN, LINEAR_EVALUATOR);
    var negativeTime = RotationChannelSampler.sample(channel, -0.1, LINEAR_EVALUATOR);
    var badEvaluator = RotationChannelSampler.sample(channel, 0.5, null);

    for (var result : List.of(badTime, negativeTime, badEvaluator)) {
      assertFalse(result.success());
      assertEquals(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
          result.diagnostics().errors().get(0).code().value());
    }
  }

  private static SourceRotationChannelIR rotations(List<SourceRotationKeyframeIR> keyframes) {
    return new SourceRotationChannelIR(keyframes, RotationOrder.ZYX);
  }

  private static SourceRotationKeyframeIR key(double time, double x, double y, double z) {
    Vec3d value = new Vec3d(x, y, z);
    return new SourceRotationKeyframeIR(
        time, value, value, InterpolationIR.LINEAR, List.of(), "test");
  }

  private static void assertQuatEquivalent(Quatd expected, Quatd actual) {
    double dot =
        expected.w() * actual.w()
            + expected.x() * actual.x()
            + expected.y() * actual.y()
            + expected.z() * actual.z();
    assertEquals(1.0, Math.abs(dot), 1e-12);
  }
}
