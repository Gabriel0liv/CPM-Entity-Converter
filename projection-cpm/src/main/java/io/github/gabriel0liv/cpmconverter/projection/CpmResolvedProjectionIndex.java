package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public record CpmResolvedProjectionIndex(
    Map<BoneId, CpmStoreId> boneStoreIds,
    Map<CubeId, CpmStoreId> cubeStoreIds,
    Map<CubeId, CpmStoreId> helperStoreIds) {
  public CpmResolvedProjectionIndex {
    boneStoreIds =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(boneStoreIds == null ? Map.of() : boneStoreIds));
    cubeStoreIds =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(cubeStoreIds == null ? Map.of() : cubeStoreIds));
    helperStoreIds =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(helperStoreIds == null ? Map.of() : helperStoreIds));
  }
}
