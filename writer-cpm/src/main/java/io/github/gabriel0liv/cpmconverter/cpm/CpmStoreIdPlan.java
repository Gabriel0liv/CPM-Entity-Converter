package io.github.gabriel0liv.cpmconverter.cpm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable mapping from logical CPM targets to persistent V1 store IDs. */
public record CpmStoreIdPlan(
    Map<CpmVanillaPart, Long> rootIds, Map<ProjectionKey, Long> elementIds) {
  public CpmStoreIdPlan {
    EnumMap<CpmVanillaPart, Long> roots = new EnumMap<>(CpmVanillaPart.class);
    roots.putAll(Objects.requireNonNull(rootIds, "rootIds"));
    rootIds = Collections.unmodifiableMap(roots);
    elementIds =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(elementIds, "elementIds")));
  }

  public long rootId(CpmVanillaPart part) {
    Long value = rootIds.get(Objects.requireNonNull(part, "part"));
    if (value == null) throw new IllegalArgumentException("unknown root " + part);
    return value;
  }

  public long elementId(ProjectionKey key) {
    Objects.requireNonNull(key, "key");
    Long value = elementIds.get(key);
    if (value == null) throw new IllegalArgumentException("unknown element " + key.value());
    return value;
  }
}
