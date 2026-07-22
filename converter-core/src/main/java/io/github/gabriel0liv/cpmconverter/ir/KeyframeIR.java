package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;

public record KeyframeIR<T>(
    double time,
    T incomingValue,
    T outgoingValue,
    InterpolationIR interpolation,
    SourceLocation source) {
  public KeyframeIR {
    if (!Double.isFinite(time) || time < 0 || interpolation == null)
      throw new IllegalArgumentException("keyframe");
  }

  /** Compatibility constructor for test fixtures; production boundaries provide source. */
  public KeyframeIR(double time, T incomingValue, T outgoingValue, InterpolationIR interpolation) {
    this(time, incomingValue, outgoingValue, interpolation, null);
  }
}
