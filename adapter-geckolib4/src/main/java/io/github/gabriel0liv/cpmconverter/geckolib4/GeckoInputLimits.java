package io.github.gabriel0liv.cpmconverter.geckolib4;

/** Resource limits applied before and during offline Gecko input parsing. */
public record GeckoInputLimits(
    long maxJsonBytes,
    int maxJsonDepth,
    int maxBones,
    int maxCubes,
    int maxKeyframes,
    double maxAnimationDurationSeconds,
    long maxPngBytes,
    long maxPngPixels) {
  private static final double DEFAULT_MAX_ANIMATION_DURATION_SECONDS = 3_600.0;
  private static final long DEFAULT_MAX_PNG_BYTES = 64L * 1024 * 1024;
  private static final long DEFAULT_MAX_PNG_PIXELS = 8_192L * 8_192L;

  private static final GeckoInputLimits DEFAULTS =
      new GeckoInputLimits(
          16L * 1024 * 1024,
          128,
          4_096,
          65_536,
          1_000_000,
          DEFAULT_MAX_ANIMATION_DURATION_SECONDS,
          DEFAULT_MAX_PNG_BYTES,
          DEFAULT_MAX_PNG_PIXELS);

  public GeckoInputLimits(
      long maxJsonBytes, int maxJsonDepth, int maxBones, int maxCubes, int maxKeyframes) {
    this(
        maxJsonBytes,
        maxJsonDepth,
        maxBones,
        maxCubes,
        maxKeyframes,
        DEFAULT_MAX_ANIMATION_DURATION_SECONDS,
        DEFAULT_MAX_PNG_BYTES,
        DEFAULT_MAX_PNG_PIXELS);
  }

  public GeckoInputLimits {
    if (maxJsonBytes <= 0
        || maxJsonDepth <= 0
        || maxBones <= 0
        || maxCubes <= 0
        || maxKeyframes <= 0
        || !Double.isFinite(maxAnimationDurationSeconds)
        || maxAnimationDurationSeconds <= 0
        || maxPngBytes <= 0
        || maxPngPixels <= 0) {
      throw new IllegalArgumentException("Gecko input limits must be finite and positive");
    }
  }

  public static GeckoInputLimits defaults() {
    return DEFAULTS;
  }
}
