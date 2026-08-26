package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.Objects;

/** CPM-facing local transform expressed in project V1 fields. */
public record CpmTransformV1(Vec3d translation, Vec3d rotationDegrees, Vec3d scale) {
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  public CpmTransformV1 {
    Objects.requireNonNull(translation, "translation");
    Objects.requireNonNull(rotationDegrees, "rotationDegrees");
    Objects.requireNonNull(scale, "scale");
  }

  public static CpmTransformV1 identity() {
    return new CpmTransformV1(Vec3d.ZERO, Vec3d.ZERO, ONE);
  }
}
