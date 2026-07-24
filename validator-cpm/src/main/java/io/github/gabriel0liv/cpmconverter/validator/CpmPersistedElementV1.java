package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;
import java.util.Objects;

public record CpmPersistedElementV1(String name, boolean show, boolean texture, int textureSize,
    CpmPersistedVec3 offset, CpmPersistedVec3 position, CpmPersistedVec3 rotation,
    CpmPersistedVec3 size, CpmPersistedVec3 renderScale, CpmPersistedVec3 scale,
    CpmPersistedUvV1 uv, String color, boolean mirror, double mcScale, boolean glow,
    boolean recolor, boolean hidden, boolean singleTexture, boolean extrude, boolean locked,
    int nameColor, long storeId, List<CpmPersistedElementV1> children, int preOrderIndex,
    int depth, String pointer) {
  public CpmPersistedElementV1 {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(offset, "offset");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(rotation, "rotation");
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(renderScale, "renderScale");
    Objects.requireNonNull(scale, "scale");
    Objects.requireNonNull(uv, "uv");
    Objects.requireNonNull(color, "color");
    Objects.requireNonNull(children, "children");
    Objects.requireNonNull(pointer, "pointer");
    if (!Double.isFinite(mcScale)) throw new IllegalArgumentException("mcScale");
    children = List.copyOf(children);
  }
}
