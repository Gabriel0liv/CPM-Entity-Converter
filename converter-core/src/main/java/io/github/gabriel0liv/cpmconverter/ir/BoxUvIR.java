package io.github.gabriel0liv.cpmconverter.ir;

/** Gecko box-UV origin. Fractional coordinates are preserved exactly through the IR. */
public record BoxUvIR(double u, double v) implements UvIR {
  public BoxUvIR {
    if (!Double.isFinite(u) || !Double.isFinite(v)) {
      throw new IllegalArgumentException("UV values must be finite");
    }
  }
}
