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
    RotationContinuity continuity = new RotationContinuity(new Vec3d(350, 0, 0));
    RotationContinuity next = continuity.unwrapNear(new Vec3d(10, 0, 0));
    assertEquals(350, next.sourceEulerHint().x());
    assertEquals(-1, next.winding().x());
    assertEquals(10, next.previousOutputEuler().orElseThrow().x());
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
}
