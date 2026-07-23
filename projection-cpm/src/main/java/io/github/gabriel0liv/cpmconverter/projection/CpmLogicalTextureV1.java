package io.github.gabriel0liv.cpmconverter.projection;

public record CpmLogicalTextureV1(
    String logicalPath, int width, int height, String skinType, boolean customGridSize) {
  public CpmLogicalTextureV1 {
    if (logicalPath == null
        || logicalPath.isBlank()
        || width <= 0
        || height <= 0
        || skinType == null) throw new IllegalArgumentException("texture");
  }
}
