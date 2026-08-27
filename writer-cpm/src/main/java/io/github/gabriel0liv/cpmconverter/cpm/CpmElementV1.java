package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.ir.UvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import java.util.Objects;

/** Immutable logical CPM element before store IDs and JSON serialization are assigned. */
public record CpmElementV1(
    ProjectionKey key,
    String name,
    CpmElementKind kind,
    CpmTransformV1 transform,
    Vec3d offset,
    Vec3d size,
    double mcScale,
    boolean mirror,
    boolean texture,
    boolean show,
    boolean hidden,
    UvIR uv,
    List<CpmElementV1> children) {
  public CpmElementV1 {
    Objects.requireNonNull(key, "key");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(transform, "transform");
    Objects.requireNonNull(offset, "offset");
    Objects.requireNonNull(size, "size");
    if (!Double.isFinite(mcScale)) throw new IllegalArgumentException("mcScale");
    if (kind == CpmElementKind.CUBE && uv == null) throw new IllegalArgumentException("cube uv");
    children = List.copyOf(children == null ? List.of() : children);
  }
}
