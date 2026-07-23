package io.github.gabriel0liv.cpmconverter.validator;

public record CpmPersistedTextureV1(String entryName, CpmPersistedSize2i logicalGridSize,
    boolean customGridSize, CpmPngMetadata png) {
  public CpmPersistedTextureV1(String path, int width, int height, boolean customGridSize) {
    this(path, width > 0 && height > 0 ? new CpmPersistedSize2i(width, height) : null, customGridSize,
        width > 0 && height > 0 ? new CpmPngMetadata(width, height, 8, 6, 0) : null);
  }
  public String path() { return entryName; }
  public int width() { return png == null ? 0 : png.width(); }
  public int height() { return png == null ? 0 : png.height(); }
}
