package io.github.gabriel0liv.cpmconverter.ir;

public record FaceUvIR(double u, double v, double width, double height) {
  public FaceUvIR {
    if (!Double.isFinite(u) || !Double.isFinite(v) || !Double.isFinite(width) || !Double.isFinite(height)) throw new IllegalArgumentException("face UV");
  }
}
