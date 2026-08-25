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
    return new RotationContinuity(
        sourceEulerHint,
        new Vec3i(
            (int) Math.round((previous.x() - sourceEulerHint.x()) / 360),
            (int) Math.round((previous.y() - sourceEulerHint.y()) / 360),
            (int) Math.round((previous.z() - sourceEulerHint.z()) / 360)),
        Optional.of(previous));
  }

  /**
   * Resolves a principal ZYX Euler representation to the equivalent branch nearest the previous
   * emitted value, or the source-authored hint on the first sample.
   */
  public Vec3d resolveDegrees(Vec3d principal) {
    if (principal == null) throw new IllegalArgumentException("principal");
    Vec3d reference = previousOutputEuler.orElse(sourceEulerHint);
    Vec3d direct = unwrapAxesNear(principal, reference);
    Vec3d alternate =
        unwrapAxesNear(
            new Vec3d(principal.x() + 180, 180 - principal.y(), principal.z() + 180), reference);
    return distanceSquared(direct, reference) <= distanceSquared(alternate, reference)
        ? direct
        : alternate;
  }

  public RotationContinuity withOutput(Vec3d output) {
    if (output == null) throw new IllegalArgumentException("output");
    return new RotationContinuity(
        sourceEulerHint,
        new Vec3i(
            (int) Math.round((output.x() - sourceEulerHint.x()) / 360),
            (int) Math.round((output.y() - sourceEulerHint.y()) / 360),
            (int) Math.round((output.z() - sourceEulerHint.z()) / 360)),
        Optional.of(output));
  }

  private static Vec3d unwrapAxesNear(Vec3d value, Vec3d reference) {
    return new Vec3d(
        unwrapAxisNear(value.x(), reference.x()),
        unwrapAxisNear(value.y(), reference.y()),
        unwrapAxisNear(value.z(), reference.z()));
  }

  private static double unwrapAxisNear(double value, double reference) {
    return value + 360 * Math.round((reference - value) / 360);
  }

  private static double distanceSquared(Vec3d a, Vec3d b) {
    double dx = a.x() - b.x();
    double dy = a.y() - b.y();
    double dz = a.z() - b.z();
    return dx * dx + dy * dy + dz * dz;
  }
}
