package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;

public record BoneTrackIR(
    BoneId bone,
    ChannelIR<Vec3d> position,
    SourceRotationChannelIR rotation,
    ChannelIR<Vec3d> scale,
    TransformMode mode,
    TransformSpace space,
    SourceLocation source) {
  public BoneTrackIR {
    if (bone == null || mode == null || space == null) throw new IllegalArgumentException("track");
  }

  /** Compatibility constructor for test fixtures; production boundaries provide source. */
  public BoneTrackIR(
      BoneId bone,
      ChannelIR<Vec3d> position,
      SourceRotationChannelIR rotation,
      ChannelIR<Vec3d> scale,
      TransformMode mode,
      TransformSpace space) {
    this(bone, position, rotation, scale, mode, space, null);
  }
}
