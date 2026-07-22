package io.github.gabriel0liv.cpmconverter.geckolib;

public record PngValidationRequest(PngParserLimits limits) {
  public PngValidationRequest {
    if (limits == null) limits = PngParserLimits.defaults();
  }

  public static PngValidationRequest defaults() {
    return new PngValidationRequest(PngParserLimits.defaults());
  }
}
