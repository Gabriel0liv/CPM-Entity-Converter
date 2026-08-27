package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import java.util.Objects;

/** Stable logical key for static projection targets before deterministic store IDs are assigned. */
public record ProjectionKey(String value) {
  public ProjectionKey {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
  }

  public static ProjectionKey entityRoot() {
    return new ProjectionKey("ENTITY_ROOT");
  }

  public static ProjectionKey bone(BoneId id) {
    return new ProjectionKey("BONE:" + Objects.requireNonNull(id, "id").value());
  }

  public static ProjectionKey cubeHelper(CubeId id) {
    return new ProjectionKey("CUBE_HELPER:" + Objects.requireNonNull(id, "id").value());
  }

  public static ProjectionKey cube(CubeId id) {
    return new ProjectionKey("CUBE:" + Objects.requireNonNull(id, "id").value());
  }
}
