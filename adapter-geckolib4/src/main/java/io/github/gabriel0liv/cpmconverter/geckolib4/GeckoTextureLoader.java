package io.github.gabriel0liv.cpmconverter.geckolib4;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Loads a Gecko texture without re-encoding its source PNG bytes. */
public final class GeckoTextureLoader {
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
  };

  public Result<TextureIR> load(Path path, int declaredWidth, int declaredHeight) {
    if (path == null) return failure("texture path is null");
    if (declaredWidth <= 0 || declaredHeight <= 0) {
      return failure("declared texture dimensions must be positive");
    }

    try {
      byte[] bytes = Files.readAllBytes(path);
      if (!hasPngSignature(bytes)) return failure("texture is not a PNG: " + sourcePath(path));

      BufferedImage image;
      try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
        image = ImageIO.read(input);
      }
      if (image == null) return failure("PNG cannot be decoded: " + sourcePath(path));
      if (image.getWidth() != declaredWidth || image.getHeight() != declaredHeight) {
        return failure(
            "PNG dimensions "
                + image.getWidth()
                + "x"
                + image.getHeight()
                + " do not match declared grid "
                + declaredWidth
                + "x"
                + declaredHeight);
      }

      return Result.success(
          new TextureIR(
              sourcePath(path),
              bytes,
              declaredWidth,
              declaredHeight,
              sourcePath(path)),
          java.util.List.of());
    } catch (Exception exception) {
      String detail = exception.getMessage();
      return failure(detail == null ? "PNG cannot be read" : "PNG cannot be read: " + detail);
    }
  }

  private boolean hasPngSignature(byte[] bytes) {
    if (bytes.length < PNG_SIGNATURE.length) return false;
    for (int index = 0; index < PNG_SIGNATURE.length; index++) {
      if (bytes[index] != PNG_SIGNATURE[index]) return false;
    }
    return true;
  }

  private String sourcePath(Path path) {
    Path fileName = path.getFileName();
    return fileName == null ? "texture.png" : fileName.toString();
  }

  private <T> Result<T> failure(String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, DiagnosticCodes.PNG_INVALID, message));
  }
}
