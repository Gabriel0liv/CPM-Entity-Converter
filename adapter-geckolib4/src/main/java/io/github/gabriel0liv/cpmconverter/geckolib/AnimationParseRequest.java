package io.github.gabriel0liv.cpmconverter.geckolib;

public record AnimationParseRequest(AnimationParserLimits limits) {
  public AnimationParseRequest {
    if (limits == null) limits = AnimationParserLimits.defaults();
  }

  public static AnimationParseRequest defaults() {
    return new AnimationParseRequest(AnimationParserLimits.defaults());
  }
}
