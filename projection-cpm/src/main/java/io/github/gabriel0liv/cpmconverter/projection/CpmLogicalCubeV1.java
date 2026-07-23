package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record CpmLogicalCubeV1(
    Vec3d offset,
    Vec3d size,
    Vec3d renderScale,
    Vec3d meshScale,
    boolean texture,
    int textureSize,
    UvIR uv,
    String color,
    boolean mirror,
    double mcScale,
    boolean show,
    boolean hidden,
    boolean glow,
    boolean recolor) {
  public CpmLogicalCubeV1 {
    if (offset == null || size == null || uv == null || color == null)
      throw new IllegalArgumentException("cube");
  }
}
