package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Validates PNG bytes without re-encoding or retaining image objects in the IR. */
public final class PngTextureValidator {
  private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

  public Result<ValidatedPng> validate(Path path, PngValidationRequest request) {
    if (path == null) return Result.failure(error("PNG path is required", null, "/"));
    SourcePath source = new SourcePath(path.getFileName().toString());
    return validate(path, source, request);
  }

  public Result<ValidatedPng> validate(
      Path path, SourcePath logicalSource, PngValidationRequest request) {
    if (path == null || logicalSource == null)
      return Result.failure(error("PNG path is required", null, "/"));
    SourcePath source = logicalSource;
    try {
      if (!Files.isRegularFile(path) || !Files.isReadable(path))
        return Result.failure(error("PNG file is not readable", source, "/"));
      long size = Files.size(path);
      var limits = request == null ? PngParserLimits.defaults() : request.limits();
      if (size > limits.maxBytes())
        return Result.failure(limit(source, "/", "maxBytes", limits.maxBytes(), size));
      byte[] bytes = Files.readAllBytes(path);
      if (bytes.length < SIGNATURE.length
          || !java.util.Arrays.equals(SIGNATURE, java.util.Arrays.copyOf(bytes, 8)))
        return Result.failure(error("invalid PNG signature", source, "/signature"));
      if (bytes.length < 33
          || bytes[12] != 'I'
          || bytes[13] != 'H'
          || bytes[14] != 'D'
          || bytes[15] != 'R') return Result.failure(error("PNG IHDR is missing", source, "/IHDR"));
      int width = readInt(bytes, 16), height = readInt(bytes, 20);
      if (width <= 0 || height <= 0)
        return Result.failure(
            error(
                "PNG dimensions must be positive",
                source,
                width <= 0 ? "/IHDR/width" : "/IHDR/height"));
      if (width > limits.maxWidth())
        return Result.failure(limit(source, "/IHDR/width", "maxWidth", limits.maxWidth(), width));
      if (height > limits.maxHeight())
        return Result.failure(
            limit(source, "/IHDR/height", "maxHeight", limits.maxHeight(), height));
      long pixels = (long) width * height;
      if (pixels > limits.maxPixels())
        return Result.failure(limit(source, "/pixels", "maxPixels", limits.maxPixels(), pixels));
      if (ImageIO.read(new java.io.ByteArrayInputStream(bytes)) == null)
        return Result.failure(error("PNG decode failed", source, "/decode"));
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      return Result.success(new ValidatedPng(source, width, height, bytes.length, hash));
    } catch (IOException | java.security.GeneralSecurityException ex) {
      return Result.failure(error("PNG read failed: " + ex.getMessage(), source, "/"));
    }
  }

  private static int readInt(byte[] b, int i) {
    return ((b[i] & 255) << 24)
        | ((b[i + 1] & 255) << 16)
        | ((b[i + 2] & 255) << 8)
        | (b[i + 3] & 255);
  }

  private static SourceLocation location(SourcePath source, String pointer) {
    return new SourceLocation(source, null, null, pointer, null);
  }

  private static Diagnostic error(String msg, SourcePath source, String pointer) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(DiagnosticCodes.PNG_INVALID),
        source == null ? null : location(source, pointer),
        msg,
        "provide a valid PNG file",
        null,
        null,
        new java.util.TreeMap<>());
  }

  private static Diagnostic limit(
      SourcePath source, String pointer, String name, long limit, long observed) {
    var context = new java.util.TreeMap<String, String>();
    context.put("limitName", name);
    context.put("limit", Long.toString(limit));
    context.put("observed", Long.toString(observed));
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_LIMIT_EXCEEDED),
        location(source, pointer),
        "PNG limit exceeded",
        "reduce the PNG size",
        null,
        null,
        context);
  }
}
