package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;

public record KeyframeIR<T>(
    double time,
    T incomingValue,
    T outgoingValue,
    EasingIR easingFromPrevious,
    SourceLocation source) {
  public KeyframeIR {
    if (!Double.isFinite(time) || time < 0 || easingFromPrevious == null || source == null)
      throw new IllegalArgumentException("keyframe");
  }

  public InterpolationIR interpolation() {
    return InterpolationIR.LINEAR;
  }

  public KeyframeIR(
      double time,
      T incomingValue,
      T outgoingValue,
      InterpolationIR interpolation,
      SourceLocation source) {
    this(time, incomingValue, outgoingValue, EasingIR.linear(), source);
    if (interpolation == null) throw new IllegalArgumentException("interpolation");
  }
}
