package io.github.gabriel0liv.cpmconverter.ir;

/** Gecko face UV rectangle. Signed extents are preserved because their sign encodes orientation. */
public record FaceUvIR(double u, double v, double width, double height) {
  public FaceUvIR {
    if (!Double.isFinite(u)
        || !Double.isFinite(v)
        || !Double.isFinite(width)
        || !Double.isFinite(height)) {
      throw new IllegalArgumentException("UV values must be finite");
    }
  }
}
