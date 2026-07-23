package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;

public record CpmPersistedElementV1(String name, boolean show, boolean texture, int textureSize,
    CpmPersistedVec3 offset, CpmPersistedVec3 position, CpmPersistedVec3 rotation,
    CpmPersistedVec3 size, CpmPersistedVec3 renderScale, CpmPersistedVec3 scale,
    CpmPersistedUvV1 uv, String color, boolean mirror, double mcScale, boolean glow,
    boolean recolor, boolean hidden, boolean singleTexture, boolean extrude, boolean locked,
    int nameColor, long storeId, List<CpmPersistedElementV1> children, int preOrderIndex,
    int depth, String pointer) {
  public CpmPersistedElementV1 {
    if (name == null) throw new IllegalArgumentException("name");
    if (!Double.isFinite(mcScale)) throw new IllegalArgumentException("mcScale");
    children = List.copyOf(children == null ? List.of() : children);
  }
  public CpmPersistedElementV1(String name, Long storeId, List<CpmPersistedElementV1> children,
      int preOrderIndex, int depth, String pointer) {
    this(name, false, false, 1, null, null, null, null, null, null, null, null, false, 1.0,
        false, false, false, false, false, false, 0, storeId == null ? -1L : storeId,
        children, preOrderIndex, depth, pointer);
  }
}
