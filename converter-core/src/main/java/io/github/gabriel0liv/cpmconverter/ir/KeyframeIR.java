package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;

public record KeyframeIR<T>(
    double time,
    T incomingValue,
    T outgoingValue,
    InterpolationIR interpolation,
    SourceLocation source) {
  public KeyframeIR {
    if (!Double.isFinite(time) || time < 0 || interpolation == null || source == null)
      throw new IllegalArgumentException("keyframe");
  }
}
