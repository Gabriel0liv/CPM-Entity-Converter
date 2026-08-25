package io.github.gabriel0liv.cpmconverter.geckolib4;

/** Resource limits applied before and during offline Gecko input parsing. */
public record GeckoInputLimits(
    long maxJsonBytes, int maxJsonDepth, int maxBones, int maxCubes, int maxKeyframes) {
  private static final GeckoInputLimits DEFAULTS =
      new GeckoInputLimits(16L * 1024 * 1024, 128, 4_096, 65_536, 1_000_000);

  public GeckoInputLimits {
    if (maxJsonBytes <= 0
        || maxJsonDepth <= 0
        || maxBones <= 0
        || maxCubes <= 0
        || maxKeyframes <= 0) {
      throw new IllegalArgumentException("Gecko input limits must be positive");
    }
  }

  public static GeckoInputLimits defaults() {
    return DEFAULTS;
  }
}
