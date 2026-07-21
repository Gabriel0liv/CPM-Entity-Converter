package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record BoneTrackIR(
    BoneId bone,
    ChannelIR<Vec3d> position,
    SourceRotationChannelIR rotation,
    ChannelIR<Vec3d> scale,
    TransformMode mode,
    TransformSpace space) {
  public BoneTrackIR {
    if (bone == null || mode == null || space == null) throw new IllegalArgumentException("track");
  }
}
