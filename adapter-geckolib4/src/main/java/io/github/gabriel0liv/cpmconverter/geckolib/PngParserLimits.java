package io.github.gabriel0liv.cpmconverter.geckolib;

public record PngParserLimits(long maxBytes, int maxWidth, int maxHeight, long maxPixels) {
  public PngParserLimits {
    if (maxBytes <= 0 || maxWidth <= 0 || maxHeight <= 0 || maxPixels <= 0) {
      throw new IllegalArgumentException("positive limits required");
    }
  }

  public static PngParserLimits defaults() {
    return new PngParserLimits(16L * 1024 * 1024, 8192, 8192, 67_108_864L);
  }
}
