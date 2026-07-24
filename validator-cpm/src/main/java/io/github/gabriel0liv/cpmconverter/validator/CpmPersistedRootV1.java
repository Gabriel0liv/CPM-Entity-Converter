package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;

public record CpmPersistedRootV1(String id, long effectiveStoreId, boolean show,
    boolean showInEditor, boolean locked, CpmPersistedVec3 position, CpmPersistedVec3 rotation,
    boolean duplicate, boolean disableVanillaAnimation, String name, int nameColor,
    List<CpmPersistedElementV1> children, String pointer) {
  public CpmPersistedRootV1 {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
    children = List.copyOf(children == null ? List.of() : children);
    position = position == null ? new CpmPersistedVec3(0, 0, 0) : position;
    rotation = rotation == null ? new CpmPersistedVec3(0, 0, 0) : rotation;
    name = name == null ? "" : name;
  }
}
