package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record SourceRotationKeyframeIR(
    double timeSeconds,
    Vec3d incomingValue,
    Vec3d outgoingValue,
    InterpolationIR interpolationAfter,
    String source) {
  public SourceRotationKeyframeIR {
    if (!Double.isFinite(timeSeconds)
        || timeSeconds < 0
        || incomingValue == null
        || outgoingValue == null
        || interpolationAfter == null
        || source == null) throw new IllegalArgumentException("rotation keyframe");
  }
}
