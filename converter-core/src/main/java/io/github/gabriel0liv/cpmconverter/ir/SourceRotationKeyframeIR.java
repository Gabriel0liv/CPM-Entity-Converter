package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record SourceRotationKeyframeIR(
    double timeSeconds,
    Vec3d incomingValue,
    Vec3d outgoingValue,
    InterpolationIR interpolationAfter,
    SourceLocation source) {
  public SourceRotationKeyframeIR {
    if (!Double.isFinite(timeSeconds)
        || timeSeconds < 0
        || incomingValue == null
        || outgoingValue == null
        || interpolationAfter == null
        || source == null) throw new IllegalArgumentException("rotation keyframe");
  }

  @Deprecated
  public SourceRotationKeyframeIR(
      double timeSeconds,
      Vec3d incomingValue,
      Vec3d outgoingValue,
      InterpolationIR interpolationAfter,
      String source) {
    this(
        timeSeconds,
        incomingValue,
        outgoingValue,
        interpolationAfter,
        SourceLocation.of(
            new SourcePath(source == null || source.isBlank() ? "legacy/animation" : source)));
  }
}
