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
}
