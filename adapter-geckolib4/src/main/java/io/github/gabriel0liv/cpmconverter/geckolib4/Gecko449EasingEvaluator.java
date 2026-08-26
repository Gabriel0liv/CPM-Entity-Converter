package io.github.gabriel0liv.cpmconverter.geckolib4;

import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/** Numeric reproduction of the built-in easing functions registered by GeckoLib 4.4.9. */
public final class Gecko449EasingEvaluator {
  private Gecko449EasingEvaluator() {}

  public static double apply(InterpolationIR interpolation, List<Double> easingArgs, double time) {
    if (interpolation == null || easingArgs == null || !Double.isFinite(time)) {
      throw new IllegalArgumentException("easing input");
    }
    if (time >= 1) return 1;

    Double arg = easingArgs.isEmpty() ? null : easingArgs.get(0);
    return switch (interpolation) {
      case LINEAR -> time;
      case STEP -> step(arg).applyAsDouble(time);
      case EASE_IN_SINE -> sine(time);
      case EASE_OUT_SINE -> easeOut(Gecko449EasingEvaluator::sine, time);
      case EASE_IN_OUT_SINE -> easeInOut(Gecko449EasingEvaluator::sine, time);
      case EASE_IN_QUAD -> quadratic(time);
      case EASE_OUT_QUAD -> easeOut(Gecko449EasingEvaluator::quadratic, time);
      case EASE_IN_OUT_QUAD -> easeInOut(Gecko449EasingEvaluator::quadratic, time);
      case EASE_IN_CUBIC -> cubic(time);
      case EASE_OUT_CUBIC -> easeOut(Gecko449EasingEvaluator::cubic, time);
      case EASE_IN_OUT_CUBIC -> easeInOut(Gecko449EasingEvaluator::cubic, time);
      case EASE_IN_QUART -> pow(4).applyAsDouble(time);
      case EASE_OUT_QUART -> easeOut(pow(4), time);
      case EASE_IN_OUT_QUART -> easeInOut(pow(4), time);
        // GeckoLib 4.4.9 registers easeinquint with pow(4); this is intentional compatibility.
      case EASE_IN_QUINT -> pow(4).applyAsDouble(time);
      case EASE_OUT_QUINT -> easeOut(pow(5), time);
      case EASE_IN_OUT_QUINT -> easeInOut(pow(5), time);
      case EASE_IN_EXPO -> exp(time);
      case EASE_OUT_EXPO -> easeOut(Gecko449EasingEvaluator::exp, time);
      case EASE_IN_OUT_EXPO -> easeInOut(Gecko449EasingEvaluator::exp, time);
      case EASE_IN_CIRC -> circle(time);
      case EASE_OUT_CIRC -> easeOut(Gecko449EasingEvaluator::circle, time);
      case EASE_IN_OUT_CIRC -> easeInOut(Gecko449EasingEvaluator::circle, time);
      case EASE_IN_BACK -> back(arg).applyAsDouble(time);
      case EASE_OUT_BACK -> easeOut(back(arg), time);
      case EASE_IN_OUT_BACK -> easeInOut(back(arg), time);
      case EASE_IN_ELASTIC -> elastic(arg).applyAsDouble(time);
      case EASE_OUT_ELASTIC -> easeOut(elastic(arg), time);
      case EASE_IN_OUT_ELASTIC -> easeInOut(elastic(arg), time);
      case EASE_IN_BOUNCE -> bounce(arg).applyAsDouble(time);
      case EASE_OUT_BOUNCE -> easeOut(bounce(arg), time);
      case EASE_IN_OUT_BOUNCE -> easeInOut(bounce(arg), time);
      case CATMULLROM -> easeInOut(Gecko449EasingEvaluator::catmullRom, time);
      case CUSTOM -> throw new IllegalArgumentException("custom easing is not available offline");
    };
  }

  private static double easeOut(DoubleUnaryOperator function, double time) {
    return 1 - function.applyAsDouble(1 - time);
  }

  private static double easeInOut(DoubleUnaryOperator function, double time) {
    if (time < 0.5d) return function.applyAsDouble(time * 2d) / 2d;
    return 1 - function.applyAsDouble((1 - time) * 2d) / 2d;
  }

  private static double quadratic(double n) {
    return n * n;
  }

  private static double cubic(double n) {
    return n * n * n;
  }

  private static double sine(double n) {
    return 1 - Math.cos(n * Math.PI / 2f);
  }

  private static double circle(double n) {
    return 1 - Math.sqrt(1 - n * n);
  }

  private static double exp(double n) {
    return Math.pow(2, 10 * (n - 1));
  }

  private static double catmullRom(double n) {
    return 0.5f
        * (2.0f * (n + 1)
            + ((n + 2) - n)
            + (2.0f * n - 5.0f * (n + 1) + 4.0f * (n + 2) - (n + 3))
            + (3.0f * (n + 1) - n - 3.0f * (n + 2) + (n + 3)));
  }

  private static DoubleUnaryOperator elastic(Double n) {
    double value = n == null ? 1 : n;
    return t -> 1 - Math.pow(Math.cos(t * Math.PI / 2f), 3) * Math.cos(t * value * Math.PI);
  }

  private static DoubleUnaryOperator bounce(Double n) {
    double value = n == null ? 0.5d : n;
    DoubleUnaryOperator one = x -> 121f / 16f * x * x;
    DoubleUnaryOperator two = x -> 121f / 4f * value * Math.pow(x - 6f / 11f, 2) + 1 - value;
    DoubleUnaryOperator three =
        x -> 121 * value * value * Math.pow(x - 9f / 11f, 2) + 1 - value * value;
    DoubleUnaryOperator four =
        x -> 484 * value * value * value * Math.pow(x - 10.5f / 11f, 2) + 1 - value * value * value;
    return t ->
        Math.min(
            Math.min(one.applyAsDouble(t), two.applyAsDouble(t)),
            Math.min(three.applyAsDouble(t), four.applyAsDouble(t)));
  }

  private static DoubleUnaryOperator back(Double n) {
    double value = n == null ? 1.70158d : n * 1.70158d;
    return t -> t * t * ((value + 1) * t - value);
  }

  private static DoubleUnaryOperator pow(double n) {
    return t -> Math.pow(t, n);
  }

  private static DoubleUnaryOperator step(Double n) {
    double value = n == null ? 2 : n;
    if (value < 2) throw new IllegalArgumentException("Steps must be >= 2, got: " + value);
    int steps = (int) value;
    return t -> {
      double result = 0;
      if (t < 0) return result;
      double stepLength = 1 / (double) steps;
      if (t > (result = (steps - 1) * stepLength)) return result;
      int leftBorderIndex = 0;
      int rightBorderIndex = steps - 1;
      while (rightBorderIndex - leftBorderIndex != 1) {
        int testIndex = leftBorderIndex + (rightBorderIndex - leftBorderIndex) / 2;
        if (t >= testIndex * stepLength) leftBorderIndex = testIndex;
        else rightBorderIndex = testIndex;
      }
      return leftBorderIndex * stepLength;
    };
  }
}
