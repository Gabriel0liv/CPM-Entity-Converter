package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RotationContinuityTest {
  @Test
  void windingIsIndependentOfQuaternion() {
    RotationContinuity continuity =
        new RotationContinuity(new Vec3d(350, 0, 0), new Vec3i(1, 0, 0), Optional.empty());
    assertEquals(1, continuity.winding().x());
    assertEquals(350, continuity.sourceEulerHint().x());
  }

  @Test
  void crossingMinus179Chooses181NearPrevious179() {
    RotationContinuity continuity =
        new RotationContinuity(
            new Vec3d(179, 0, 0), new Vec3i(0, 0, 0), Optional.of(new Vec3d(179, 0, 0)));

    Vec3d resolved = continuity.resolveDegrees(new Vec3d(-179, 0, 0));

    assertEquals(181, resolved.x(), 1e-9);
  }

  @Test
  void sourceHintPreservesTwoAuthoredTurns() {
    RotationContinuity continuity = new RotationContinuity(new Vec3d(720, 0, 0));

    Vec3d resolved = continuity.resolveDegrees(new Vec3d(0, 0, 0));

    assertEquals(720, resolved.x(), 1e-9);
  }

  @Test
  void authoredZeroToThreeSixtyToSevenTwentySequenceKeepsWinding() {
    Optional<Vec3d> previous = Optional.empty();
    for (double authored : new double[] {0, 360, 720}) {
      RotationContinuity continuity =
          new RotationContinuity(
              new Vec3d(authored, 0, 0), new Vec3i(0, 0, 0), previous);
      Vec3d resolved = continuity.resolveDegrees(new Vec3d(0, 0, 0));

      assertEquals(authored, resolved.x(), 1e-9);
      previous = Optional.of(resolved);
    }
  }

  @Test
  void choosesEquivalentZyxBranchNearestToSourceHint() {
    RotationContinuity continuity = new RotationContinuity(new Vec3d(180, 100, 180));

    Vec3d resolved = continuity.resolveDegrees(new Vec3d(0, 80, 0));

    assertEquals(180, resolved.x(), 1e-9);
    assertEquals(100, resolved.y(), 1e-9);
    assertEquals(180, resolved.z(), 1e-9);
  }

  @Test
  void updatingOutputMakesNextResolutionContinuous() {
    RotationContinuity first = new RotationContinuity(new Vec3d(179, 0, 0));
    Vec3d firstResolved = first.resolveDegrees(new Vec3d(179, 0, 0));
    RotationContinuity next = first.withOutput(firstResolved);

    assertEquals(181, next.resolveDegrees(new Vec3d(-179, 0, 0)).x(), 1e-9);
  }
}
