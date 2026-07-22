package io.github.gabriel0liv.cpmconverter.ir;

public record BoxUvIR(double u, double v) implements UvIR {
  public BoxUvIR {
    if (!Double.isFinite(u) || !Double.isFinite(v)) throw new IllegalArgumentException("UV");
  }
}
