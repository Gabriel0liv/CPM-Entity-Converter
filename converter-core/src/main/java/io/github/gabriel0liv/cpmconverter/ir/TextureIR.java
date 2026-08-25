package io.github.gabriel0liv.cpmconverter.ir;

import java.util.Arrays;

/** Texture metadata plus the exact source PNG payload used by lossless projection. */
public record TextureIR(String path, byte[] pngBytes, int width, int height, String provenance) {
  public TextureIR {
    if (path == null || path.isBlank() || width <= 0 || height <= 0) {
      throw new IllegalArgumentException("texture");
    }
    if (pngBytes == null) throw new IllegalArgumentException("pngBytes");
    if (provenance == null) throw new IllegalArgumentException("provenance");
    pngBytes = Arrays.copyOf(pngBytes, pngBytes.length);
  }

  /** Compatibility constructor for metadata-only fixtures created before PNG ingestion. */
  public TextureIR(String path, int width, int height) {
    this(path, new byte[0], width, height, path);
  }

  @Override
  public byte[] pngBytes() {
    return Arrays.copyOf(pngBytes, pngBytes.length);
  }
}
