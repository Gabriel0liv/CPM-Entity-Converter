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
import java.util.Iterator;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Loads a Gecko texture without re-encoding its source PNG bytes. */
public final class GeckoTextureLoader {
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
  };

  private final GeckoInputLimits limits;

  public GeckoTextureLoader() {
    this(GeckoInputLimits.defaults());
  }

  public GeckoTextureLoader(GeckoInputLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  public Result<TextureIR> load(Path path, int declaredWidth, int declaredHeight) {
    if (path == null) return failure("texture path is null");
    if (declaredWidth <= 0 || declaredHeight <= 0) {
      return failure("declared texture dimensions must be positive");
    }

    long declaredPixels = (long) declaredWidth * declaredHeight;
    if (declaredPixels > limits.maxPngPixels()) {
      return limitFailure(
          "declared texture grid contains "
              + declaredPixels
              + " pixels, exceeding limit "
              + limits.maxPngPixels());
    }

    try {
      long fileSize = Files.size(path);
      if (fileSize > limits.maxPngBytes()) {
        return limitFailure(
            "PNG file size " + fileSize + " bytes exceeds limit " + limits.maxPngBytes());
      }

      byte[] bytes = Files.readAllBytes(path);
      if (bytes.length > limits.maxPngBytes()) {
        return limitFailure(
            "PNG file size " + bytes.length + " bytes exceeds limit " + limits.maxPngBytes());
      }
      if (!hasPngSignature(bytes)) return failure("texture is not a PNG: " + sourcePath(path));

      try (ImageInputStream input =
          ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
        if (input == null) return failure("PNG cannot be inspected: " + sourcePath(path));
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) return failure("PNG cannot be decoded: " + sourcePath(path));

        ImageReader reader = readers.next();
        try {
          reader.setInput(input, true, true);
          int width = reader.getWidth(0);
          int height = reader.getHeight(0);
          long pixels = (long) width * height;
          if (pixels > limits.maxPngPixels()) {
            return limitFailure(
                "PNG contains " + pixels + " pixels, exceeding limit " + limits.maxPngPixels());
          }
          if (width != declaredWidth || height != declaredHeight) {
            return failure(
                "PNG dimensions "
                    + width
                    + "x"
                    + height
                    + " do not match declared grid "
                    + declaredWidth
                    + "x"
                    + declaredHeight);
          }

          BufferedImage image = reader.read(0);
          if (image == null) return failure("PNG cannot be decoded: " + sourcePath(path));
          if (image.getWidth() != width || image.getHeight() != height) {
            return failure("PNG decoded dimensions changed unexpectedly: " + sourcePath(path));
          }
        } finally {
          reader.dispose();
        }
      }

      return Result.success(
          new TextureIR(sourcePath(path), bytes, declaredWidth, declaredHeight, sourcePath(path)));
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

  private <T> Result<T> limitFailure(String message) {
    return Result.failure(
        Diagnostic.of(Severity.ERROR, DiagnosticCodes.INPUT_LIMIT_EXCEEDED, message));
  }

  private <T> Result<T> failure(String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, DiagnosticCodes.PNG_INVALID, message));
  }
}
