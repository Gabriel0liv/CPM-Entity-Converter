package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public record CpmProjectionIndex(
    Map<BoneId, CpmTargetRef> boneTargets,
    Map<CubeId, CpmTargetRef> cubeTargets,
    Map<CubeId, CpmNodeKey> helperTargets) {
  public CpmProjectionIndex {
    boneTargets =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(boneTargets == null ? Map.of() : boneTargets));
    cubeTargets =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(cubeTargets == null ? Map.of() : cubeTargets));
    helperTargets =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(helperTargets == null ? Map.of() : helperTargets));
  }
}
