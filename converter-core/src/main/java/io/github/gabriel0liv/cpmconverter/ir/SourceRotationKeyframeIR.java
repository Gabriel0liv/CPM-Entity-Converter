package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;

public record SourceRotationKeyframeIR(
    double timeSeconds,
    Vec3d incomingValue,
    Vec3d outgoingValue,
    InterpolationIR interpolationAfter,
    List<Double> easingArgsAfter,
    String source) {
  public SourceRotationKeyframeIR {
    if (!Double.isFinite(timeSeconds)
        || timeSeconds < 0
        || incomingValue == null
        || outgoingValue == null
        || interpolationAfter == null
        || easingArgsAfter == null
        || source == null) throw new IllegalArgumentException("rotation keyframe");
    for (Double arg : easingArgsAfter) {
      if (arg == null || !Double.isFinite(arg)) throw new IllegalArgumentException("easing arg");
    }
    easingArgsAfter = List.copyOf(easingArgsAfter);
  }

  public SourceRotationKeyframeIR(
      double timeSeconds,
      Vec3d incomingValue,
      Vec3d outgoingValue,
      InterpolationIR interpolationAfter,
      String source) {
    this(timeSeconds, incomingValue, outgoingValue, interpolationAfter, List.of(), source);
  }
}
