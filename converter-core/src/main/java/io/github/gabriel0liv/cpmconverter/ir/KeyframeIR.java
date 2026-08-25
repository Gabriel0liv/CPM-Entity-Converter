package io.github.gabriel0liv.cpmconverter.ir;

import java.util.List;

public record KeyframeIR<T>(
    double time,
    T incomingValue,
    T outgoingValue,
    InterpolationIR interpolation,
    List<Double> easingArgs) {
  public KeyframeIR {
    if (!Double.isFinite(time) || time < 0 || interpolation == null || easingArgs == null)
      throw new IllegalArgumentException("keyframe");
    for (Double arg : easingArgs) {
      if (arg == null || !Double.isFinite(arg)) throw new IllegalArgumentException("easing arg");
    }
    easingArgs = List.copyOf(easingArgs);
  }

  public KeyframeIR(double time, T incomingValue, T outgoingValue, InterpolationIR interpolation) {
    this(time, incomingValue, outgoingValue, interpolation, List.of());
  }
}
