package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.ir.*;

public final class GeckoEasingEvaluator {
  public double evaluateProgress(EasingIR e, double t) {
    if (e == null || !Double.isFinite(t) || t < 0 || t > 1)
      throw new IllegalArgumentException("progress");
    if (t == 1) return 1;
    return eval(e.kind(), t, e.args());
  }

  private double eval(EasingKindIR k, double t, java.util.List<Double> a) {
    if (k == EasingKindIR.LINEAR) return t;
    if (k == EasingKindIR.STEP) {
      int n = a.isEmpty() ? 2 : a.get(0).intValue();
      if (t == 1) return 1;
      return Math.floor(t * (n - 1)) / (n - 1);
    }
    return switch (k) {
      case EASE_IN_SINE -> 1 - Math.cos(t * Math.PI / 2);
      case EASE_OUT_SINE -> Math.sin(t * Math.PI / 2);
      case EASE_IN_OUT_SINE -> -(Math.cos(Math.PI * t) - 1) / 2;
      case EASE_IN_QUAD -> t * t;
      case EASE_OUT_QUAD -> 1 - (1 - t) * (1 - t);
      case EASE_IN_OUT_QUAD -> t < .5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
      case EASE_IN_CUBIC -> t * t * t;
      case EASE_OUT_CUBIC -> 1 - Math.pow(1 - t, 3);
      case EASE_IN_OUT_CUBIC -> t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
      case EASE_IN_QUART -> Math.pow(t, 4);
      case EASE_OUT_QUART -> 1 - Math.pow(1 - t, 4);
      case EASE_IN_OUT_QUART -> t < .5 ? 8 * Math.pow(t, 4) : 1 - Math.pow(-2 * t + 2, 4) / 2;
      case EASE_IN_QUINT -> Math.pow(t, 4);
      case EASE_OUT_QUINT -> 1 - Math.pow(1 - t, 5);
      case EASE_IN_OUT_QUINT -> t < .5 ? 16 * Math.pow(t, 5) : 1 - Math.pow(-2 * t + 2, 5) / 2;
      case EASE_IN_EXPO -> t == 0 ? 0 : Math.pow(2, 10 * t - 10);
      case EASE_OUT_EXPO -> t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
      case EASE_IN_OUT_EXPO ->
          t == 0
              ? 0
              : t == 1
                  ? 1
                  : t < .5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
      case EASE_IN_CIRC -> 1 - Math.sqrt(1 - t * t);
      case EASE_OUT_CIRC -> Math.sqrt(1 - (t - 1) * (t - 1));
      case EASE_IN_OUT_CIRC ->
          t < .5
              ? (1 - Math.sqrt(1 - 4 * t * t)) / 2
              : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
      case EASE_IN_BACK -> back(t, a);
      case EASE_OUT_BACK -> 1 - back(1 - t, a);
      case EASE_IN_OUT_BACK -> t < .5 ? (back(2 * t, a)) / 2 : (2 - back(2 - 2 * t, a)) / 2;
      case EASE_IN_ELASTIC -> elastic(t, a);
      case EASE_OUT_ELASTIC -> 1 - elastic(1 - t, a);
      case EASE_IN_OUT_ELASTIC -> t < .5 ? elastic(2 * t, a) / 2 : 1 - elastic(2 - 2 * t, a) / 2;
      case EASE_IN_BOUNCE -> bounce(t, a);
      case EASE_OUT_BOUNCE -> 1 - bounce(1 - t, a);
      case EASE_IN_OUT_BOUNCE -> t < .5 ? bounce(2 * t, a) / 2 : 1 - bounce(2 - 2 * t, a) / 2;
      case CATMULLROM -> t * t * (3 - 2 * t);
      default -> t;
    };
  }

  private double back(double t, java.util.List<Double> a) {
    double n = (a.isEmpty() ? 1 : a.get(0)) * 1.70158;
    return t * t * ((n + 1) * t - n);
  }

  private double elastic(double t, java.util.List<Double> a) {
    double n = a.isEmpty() ? 1 : a.get(0);
    return 1 - Math.pow(Math.cos(t * Math.PI / 2), 3) * Math.cos(t * n * Math.PI);
  }

  private double bounce(double t, java.util.List<Double> a) {
    double n = a.isEmpty() ? 0.5 : a.get(0);
    if (n <= 0) throw new IllegalArgumentException("bounce argument");
    double scaled = t / n;
    double floor = Math.floor(scaled);
    return Math.min(1, floor * n + (scaled - floor) * n);
  }
}
