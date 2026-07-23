package io.github.gabriel0liv.cpmconverter.projection;

public record CpmFaceUvV1(int sx, int sy, int ex, int ey, CpmUvRotation rotation, boolean autoUv) {
  public CpmFaceUvV1 {
    if (rotation == null) throw new IllegalArgumentException("rotation");
  }
}
