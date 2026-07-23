package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record CpmTransformV1(Vec3d position, Vec3d rotationDegrees, Vec3d scale) {
  public CpmTransformV1 {
    if (position == null || rotationDegrees == null || scale == null)
      throw new IllegalArgumentException("transform");
  }
}
