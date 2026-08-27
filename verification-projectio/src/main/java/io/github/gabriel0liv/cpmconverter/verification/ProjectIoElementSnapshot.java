package io.github.gabriel0liv.cpmconverter.verification;

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
    boolean hasFaceUv) {}
