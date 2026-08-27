package io.github.gabriel0liv.cpmconverter.verification;

/** Stable converter-owned CPM per-face UV rectangle. */
public record FaceUvSnapshot(
    int sx, int sy, int ex, int ey, String rotation, boolean autoUv) {}
