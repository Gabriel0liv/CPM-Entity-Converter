package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.math.Vec3i;
import java.util.Optional;

public record RotationContinuityIR(
    Vec3d sourceEulerHint, Vec3i winding, Optional<Vec3d> previousOutputEuler) {
  public RotationContinuityIR {
    if (sourceEulerHint == null || winding == null || previousOutputEuler == null)
      throw new IllegalArgumentException("rotation continuity");
  }

  public RotationContinuityIR(boolean continuousPerAxis) {
    this(Vec3d.ZERO, new Vec3i(0, 0, 0), Optional.empty());
  }
}
