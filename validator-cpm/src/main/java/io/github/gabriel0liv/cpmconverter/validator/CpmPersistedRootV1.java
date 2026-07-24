package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;
import java.util.Objects;

public record CpmPersistedRootV1(String id, CpmPersistedRootKind kind, boolean customPart,
    boolean duplicate, Long persistedStoreId, long effectiveStoreId, boolean show,
    boolean showInEditor, boolean locked, CpmPersistedVec3 position, CpmPersistedVec3 rotation,
    boolean disableVanillaAnimation, String name, int nameColor,
    List<CpmPersistedElementV1> children, String pointer) {
  public CpmPersistedRootV1 {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
    Objects.requireNonNull(kind, "kind");
    if (customPart && duplicate) throw new IllegalArgumentException("root cannot be custom and duplicate");
    if (kind == CpmPersistedRootKind.CUSTOM && (!customPart || duplicate)) {
      throw new IllegalArgumentException("custom root flags are incoherent");
    }
    if (kind == CpmPersistedRootKind.DUPLICATE && (customPart || !duplicate)) {
      throw new IllegalArgumentException("duplicate root flags are incoherent");
    }
    if (kind == CpmPersistedRootKind.VANILLA && (customPart || duplicate)) {
      throw new IllegalArgumentException("vanilla root flags are incoherent");
    }
    if (kind == CpmPersistedRootKind.VANILLA && persistedStoreId != null) {
      throw new IllegalArgumentException("vanilla root cannot have persisted storeID");
    }
    if (kind != CpmPersistedRootKind.VANILLA && persistedStoreId == null) {
      throw new IllegalArgumentException("persisted storeID required");
    }
    Objects.requireNonNull(children, "children");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(rotation, "rotation");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(pointer, "pointer");
    children = List.copyOf(children);
  }
}
