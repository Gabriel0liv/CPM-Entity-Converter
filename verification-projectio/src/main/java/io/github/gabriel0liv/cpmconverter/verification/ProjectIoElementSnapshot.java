package io.github.gabriel0liv.cpmconverter.verification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable converter-owned view of one element materialized by official ProjectIO. */
public record ProjectIoElementSnapshot(
    String path,
    String parentPath,
    String name,
    String type,
    long storeId,
    boolean texture,
    int textureSize,
    int u,
    int v,
    Vec3Snapshot position,
    Vec3Snapshot rotation,
    Vec3Snapshot scale,
    Vec3Snapshot meshScale,
    boolean hasFaceUv,
    Map<String, FaceUvSnapshot> faceUv) {
  public ProjectIoElementSnapshot {
    faceUv = Collections.unmodifiableMap(new LinkedHashMap<>(faceUv == null ? Map.of() : faceUv));
  }
}
