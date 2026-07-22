package io.github.gabriel0liv.cpmconverter.ir;

import java.util.List;

public record EasingIR(EasingKindIR kind, List<Double> args) {
  public EasingIR {
    if (kind == null
        || args == null
        || args.stream().anyMatch(a -> a == null || !Double.isFinite(a)))
      throw new IllegalArgumentException("invalid easing");
    args = List.copyOf(args);
  }

  public static EasingIR linear() {
    return new EasingIR(EasingKindIR.LINEAR, List.of());
  }
}
