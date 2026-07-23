package io.github.gabriel0liv.cpmconverter.validator;

public record CpmPersistedTextureV1(String entryName, CpmPersistedSize2i logicalGridSize,
    boolean customGridSize, CpmPngMetadata png) {
  public String path() { return entryName; }
  public int width() { return png == null ? 0 : png.width(); }
  public int height() { return png == null ? 0 : png.height(); }
}
