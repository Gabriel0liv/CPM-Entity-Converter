package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import java.util.List;

@FunctionalInterface
public interface InterpolationEvaluator {
  double evaluate(
      InterpolationIR interpolation, List<Double> easingArgs, double normalizedProgress);
}
