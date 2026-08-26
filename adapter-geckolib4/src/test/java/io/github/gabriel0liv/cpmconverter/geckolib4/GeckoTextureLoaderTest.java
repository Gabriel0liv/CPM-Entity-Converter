package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class GeckoTextureLoaderTest {
  private static final byte[] PNG_2X2 =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR4nGP8z8Dwn4GBgYGJAQoAHxcCAk+Uzr4AAAAASUVORK5CYII=");

  @Test
  void preservesOriginalPngBytesAndDeclaredGrid() throws Exception {
    Path png = Files.createTempFile("cpm-converter-texture-", ".png");
    Files.write(png, PNG_2X2);

    var result = new GeckoTextureLoader().load(png, 2, 2);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(2, result.value().width());
    assertEquals(2, result.value().height());
    assertArrayEquals(PNG_2X2, result.value().pngBytes());
  }

  @Test
  void rejectsPngWhoseActualDimensionsDoNotMatchDeclaredGrid() throws Exception {
    Path png = Files.createTempFile("cpm-converter-texture-mismatch-", ".png");
    Files.write(png, PNG_2X2);

    var result = new GeckoTextureLoader().load(png, 4, 2);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.PNG_INVALID);
  }

  @Test
  void rejectsUnreadablePngInsteadOfPassingBytesThrough() throws Exception {
    Path png = Files.createTempFile("cpm-converter-invalid-", ".png");
    Files.write(png, new byte[] {1, 2, 3, 4, 5});

    var result = new GeckoTextureLoader().load(png, 2, 2);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.PNG_INVALID);
  }

  @Test
  void rejectsPngBeforeReadingBytesWhenCompressedFileExceedsLimit() throws Exception {
    Path png = Files.createTempFile("cpm-converter-texture-bytes-", ".png");
    Files.write(png, PNG_2X2);
    var limits = new GeckoInputLimits(1_000_000, 64, 100, 100, 100, 60.0, 16, 1_000_000);

    var result = new GeckoTextureLoader(limits).load(png, 2, 2);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.INPUT_LIMIT_EXCEEDED);
  }

  @Test
  void rejectsActualPngPixelCountBeforeFullDecode() throws Exception {
    Path png = Files.createTempFile("cpm-converter-texture-pixels-", ".png");
    Files.write(png, PNG_2X2);
    var limits = new GeckoInputLimits(1_000_000, 64, 100, 100, 100, 60.0, 1_000_000, 1);

    var result = new GeckoTextureLoader(limits).load(png, 1, 1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.INPUT_LIMIT_EXCEEDED);
  }

  private static void assertHasCode(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result, String code) {
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> "missing diagnostic " + code + " in " + result.diagnostics().all());
  }
}
