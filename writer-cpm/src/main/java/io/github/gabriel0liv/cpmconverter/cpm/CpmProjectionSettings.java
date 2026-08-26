package io.github.gabriel0liv.cpmconverter.cpm;

/** User-controlled static projection settings validated by {@link CpmStaticProjector}. */
public record CpmProjectionSettings(
    double modelScale,
    double verticalOffset,
    boolean hideVanillaRoots,
    boolean disableVanillaAnim) {}
