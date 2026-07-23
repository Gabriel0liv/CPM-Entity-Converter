package io.github.gabriel0liv.cpmconverter.geckolib;

public record AnimationParserLimits(
    long maxBytesPerFile,
    int maxFiles,
    int maxNestingDepth,
    int maxClips,
    int maxBonesPerClip,
    int maxKeyframesPerChannel,
    int maxTotalKeyframes,
    int maxStringLength,
    int maxMolangExpressionLength,
    int maxEasingArgs) {
  public AnimationParserLimits {
    if (maxBytesPerFile <= 0
        || maxFiles <= 0
        || maxNestingDepth <= 0
        || maxClips <= 0
        || maxBonesPerClip <= 0
        || maxKeyframesPerChannel <= 0
        || maxTotalKeyframes <= 0
        || maxStringLength <= 0
        || maxMolangExpressionLength <= 0
        || maxEasingArgs <= 0)
      throw new IllegalArgumentException("animation parser limits must be positive");
  }

  public static AnimationParserLimits defaults() {
    return new AnimationParserLimits(
        32L * 1024 * 1024, 64, 128, 4096, 4096, 65536, 1_000_000, 16384, 16384, 64);
  }
}
