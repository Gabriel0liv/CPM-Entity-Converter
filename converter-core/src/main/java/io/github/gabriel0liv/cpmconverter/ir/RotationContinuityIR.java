package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.math.Vec3i;
import java.util.Optional;

/** Authorial Euler continuity retained alongside an instantaneous quaternion sample. */
public record RotationContinuityIR(
    Vec3d sourceEulerHint, Vec3i winding, Optional<Vec3d> previousOutputEuler) {
  public RotationContinuityIR {
    if (sourceEulerHint == null || winding == null || previousOutputEuler == null)
      throw new IllegalArgumentException("rotation continuity");
  }

  /** Returns the authorial Euler value represented by this continuity state. */
  public Vec3d resolvedEuler() {
    return new Vec3d(
        sourceEulerHint.x() + 360.0 * winding.x(),
        sourceEulerHint.y() + 360.0 * winding.y(),
        sourceEulerHint.z() + 360.0 * winding.z());
  }

  /** Compatibility constructor for legacy callers; new parsers must provide an explicit hint. */
  @Deprecated
  public RotationContinuityIR(boolean continuousPerAxis) {
    this(Vec3d.ZERO, new Vec3i(0, 0, 0), Optional.empty());
  }
}
