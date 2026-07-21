package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RotationContinuityTest {
  @Test
  void windingIsIndependentOfQuaternion() {
    RotationContinuity continuity =
        new RotationContinuity(
            new Vec3d(350, 0, 0), new Vec3i(1, 0, 0), java.util.Optional.empty());
    assertEquals(1, continuity.winding().x());
    assertEquals(350, continuity.sourceEulerHint().x());
  }

  @Test
  void unwrapNearRetainsAuthorialTurns() {
    RotationContinuity continuity = new RotationContinuity(new Vec3d(350, 0, 0));
    RotationContinuity next = continuity.unwrapNear(new Vec3d(710, 0, 0));
    assertEquals(1, next.winding().x());
    assertEquals(new Vec3d(710, 0, 0), next.previousOutputEuler().orElseThrow());
    assertEquals(710, next.resolvedEuler().x());
  }

  @Test
  void authorialTurnsRemainExplicitAcrossFullRange() {
    double[] angles = {180, 190, 360, 540, 720, -190, -360, -720};
    for (double angle : angles) {
      RotationContinuity continuity = new RotationContinuity(new Vec3d(angle, 0, 0));
      assertEquals(angle, continuity.sourceEulerHint().x());
      assertEquals(0, continuity.winding().x());
    }
  }

  @Test
  void crossingThreeHundredFiftyToTenUsesAuthorialWinding() {
    RotationContinuity continuity = new RotationContinuity(new Vec3d(10, 0, 0));
    RotationContinuity next = continuity.unwrapNear(new Vec3d(350, 0, 0));
    assertEquals(10, next.sourceEulerHint().x());
    assertEquals(1, next.winding().x());
    assertEquals(350, next.previousOutputEuler().orElseThrow().x());
    assertEquals(370, next.resolvedEuler().x());
  }

  @Test
  void consecutiveSamplesDoNotChooseQuaternionShortestPath() {
    RotationContinuity continuity = new RotationContinuity(new Vec3d(720, 0, 0));
    RotationContinuity first = continuity.unwrapNear(new Vec3d(710, 0, 0));
    RotationContinuity second = first.unwrapNear(new Vec3d(730, 0, 0));
    assertEquals(720, second.sourceEulerHint().x());
    assertEquals(0, first.winding().x());
    assertEquals(0, second.winding().x());
    assertEquals(730, second.previousOutputEuler().orElseThrow().x());
  }

  @Test
  void resolvesIndependentAxesWithoutChangingHints() {
    RotationContinuity continuity =
        new RotationContinuity(new Vec3d(10, -10, 0)).unwrapNear(new Vec3d(350, -370, 720));
    assertEquals(new Vec3d(370, -370, 720), continuity.resolvedEuler());
    assertEquals(new Vec3d(10, -10, 0), continuity.sourceEulerHint());
  }

  @Test
  void exactHalfTurnUsesPositiveTieBreak() {
    RotationContinuity positive =
        new RotationContinuity(new Vec3d(0, 0, 0)).unwrapNear(new Vec3d(180, 0, 0));
    RotationContinuity negative =
        new RotationContinuity(new Vec3d(0, 0, 0)).unwrapNear(new Vec3d(-180, 0, 0));
    assertEquals(0, positive.winding().x());
    assertEquals(0, negative.winding().x());
    assertEquals(0, positive.resolvedEuler().x());
    assertEquals(0, negative.resolvedEuler().x());
  }

  @Test
  void sequencePreservesPositiveAndNegativeTurns() {
    double[] positive = {170, 190, 350, 370, 540, 720};
    double previous = positive[0];
    for (int i = 1; i < positive.length; i++) {
      RotationContinuity state =
          new RotationContinuity(new Vec3d(positive[i], 0, 0))
              .unwrapNear(new Vec3d(previous, 0, 0));
      assertEquals(positive[i], state.resolvedEuler().x());
      previous = state.resolvedEuler().x();
    }
    double[] negative = {-170, -190, -350, -370, -540, -720};
    previous = negative[0];
    for (int i = 1; i < negative.length; i++) {
      RotationContinuity state =
          new RotationContinuity(new Vec3d(negative[i], 0, 0))
              .unwrapNear(new Vec3d(previous, 0, 0));
      assertEquals(negative[i], state.resolvedEuler().x());
      previous = state.resolvedEuler().x();
    }
  }

  @Test
  void rejectsNonFinitePreviousEuler() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RotationContinuity(new Vec3d(0, 0, 0)).unwrapNear(new Vec3d(Double.NaN, 0, 0)));
  }
}
