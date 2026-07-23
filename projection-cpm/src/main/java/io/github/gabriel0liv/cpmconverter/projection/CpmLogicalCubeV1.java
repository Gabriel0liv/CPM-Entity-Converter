package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record CpmLogicalCubeV1(
    Vec3d offset,
    Vec3d size,
    Vec3d renderScale,
    Vec3d meshScale,
    boolean texture,
    int textureSize,
    CpmUvV1 uv,
    String color,
    boolean mirror,
    double mcScale,
    boolean show,
    boolean hidden,
    boolean glow,
    boolean recolor) {
  public CpmLogicalCubeV1 {
    if (offset == null
        || size == null
        || renderScale == null
        || meshScale == null
        || uv == null
        || color == null
        || color.isBlank()
        || textureSize <= 0
        || !Double.isFinite(mcScale)
        || size.x() < 0
        || size.y() < 0
        || size.z() < 0) throw new IllegalArgumentException("cube");
  }
}
