package io.github.gabriel0liv.cpmconverter.math;

import java.util.Optional;

public record RotationContinuity(
    Vec3d sourceEulerHint, Vec3i winding, Optional<Vec3d> previousOutputEuler) {
  public RotationContinuity {
    if (sourceEulerHint == null || winding == null || previousOutputEuler == null)
      throw new IllegalArgumentException("rotation continuity");
  }

  public RotationContinuity(Vec3d hint) {
    this(hint, new Vec3i(0, 0, 0), Optional.empty());
  }

  public RotationContinuity unwrapNear(Vec3d previous) {
    if (previous == null || !previous.isFinite()) {
      throw new IllegalArgumentException("previous Euler must be finite");
    }
    return new RotationContinuity(
        sourceEulerHint,
        new Vec3i(
            nearestWinding(sourceEulerHint.x(), previous.x()),
            nearestWinding(sourceEulerHint.y(), previous.y()),
            nearestWinding(sourceEulerHint.z(), previous.z())),
        Optional.of(previous));
  }

  /**
   * Returns the authorial Euler value represented by this continuity state. Quaternion
   * normalization is deliberately not involved in this operation.
   */
  public Vec3d resolvedEuler() {
    return new Vec3d(
        sourceEulerHint.x() + 360.0 * winding.x(),
        sourceEulerHint.y() + 360.0 * winding.y(),
        sourceEulerHint.z() + 360.0 * winding.z());
  }

  /**
   * Chooses the closest winding to the previous sample. Exact half turns are resolved toward zero
   * (and then positive when both candidates have equal magnitude), making the tie deterministic.
   */
  private static int nearestWinding(double source, double previous) {
    double quotient = (previous - source) / 360.0;
    if (!Double.isFinite(quotient)
        || quotient > Integer.MAX_VALUE - 1.0
        || quotient < Integer.MIN_VALUE) {
      throw new IllegalArgumentException("Euler winding is outside supported range");
    }
    double lower = Math.floor(quotient);
    double upper = Math.ceil(quotient);
    double lowerDistance = Math.abs(quotient - lower);
    double upperDistance = Math.abs(upper - quotient);
    if (lowerDistance < upperDistance) {
      return (int) lower;
    }
    if (upperDistance < lowerDistance) {
      return (int) upper;
    }
    if (Math.abs(lower) < Math.abs(upper)) {
      return (int) lower;
    }
    if (Math.abs(upper) < Math.abs(lower)) {
      return (int) upper;
    }
    return (int) Math.max(lower, upper);
  }
}
