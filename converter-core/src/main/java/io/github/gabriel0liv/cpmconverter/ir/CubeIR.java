package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.math.*;

public record CubeIR(
    CubeId id,
    BoneId bone,
    Vec3d origin,
    Vec3d size,
    Vec3d pivot,
    Quatd rotation,
    double inflate,
    boolean mirror,
    UvIR uv,
    SourceLocation provenance) {
  public CubeIR {
    if (id == null
        || bone == null
        || origin == null
        || size == null
        || pivot == null
        || rotation == null
        || uv == null
        || provenance == null
        || !Double.isFinite(inflate)
        || size.x() < 0
        || size.y() < 0
        || size.z() < 0) throw new IllegalArgumentException("cube");
  }

  /**
   * Compatibility constructor for existing fixture data; decoded production cubes must carry a
   * location.
   */
  @Deprecated
  public CubeIR(
      CubeId id,
      BoneId bone,
      Vec3d origin,
      Vec3d size,
      Vec3d pivot,
      Quatd rotation,
      double inflate,
      boolean mirror,
      UvIR uv,
      String provenance) {
    this(id, bone, origin, size, pivot, rotation, inflate, mirror, uv, locationFor(provenance));
  }

  private static SourceLocation locationFor(String value) {
    return SourceLocation.of(
        new SourcePath(value == null || value.isBlank() ? "legacy/model" : value));
  }
}
