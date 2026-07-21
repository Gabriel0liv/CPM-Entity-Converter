package io.github.gabriel0liv.cpmconverter.math;

import java.util.Optional;

public record RotationContinuity(Vec3d sourceEulerHint, Vec3i winding, Optional<Vec3d> previousOutputEuler) {
    public RotationContinuity { if (sourceEulerHint == null || winding == null || previousOutputEuler == null) throw new IllegalArgumentException("rotation continuity"); }
    public RotationContinuity(Vec3d hint) { this(hint, new Vec3i(0, 0, 0), Optional.empty()); }
    public RotationContinuity unwrapNear(Vec3d previous) { return new RotationContinuity(sourceEulerHint, new Vec3i((int)Math.round((previous.x()-sourceEulerHint.x())/360), (int)Math.round((previous.y()-sourceEulerHint.y())/360), (int)Math.round((previous.z()-sourceEulerHint.z())/360)), Optional.of(previous)); }
}
