package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;

class CpmPngValidatorTest {
  @Test
  void acceptsMinimalRgba8PngAndValidatesScanline() {
    var result = new CpmPngValidator().validate(png(1, 1, new byte[] {0, (byte) 255, 0, 0, (byte) 255}), CpmArtifactLimits.defaults());
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(1, result.value().width());
    assertEquals(1, result.value().height());
  }

  @Test
  void rejectsUnsupportedColorTypeAndBadFilter() {
    var color = new CpmPngValidator().validate(pngWithColorType(1, 1, 2, new byte[] {0, 0, 0, 0}), CpmArtifactLimits.defaults());
    assertFalse(color.success());
    var filter = new CpmPngValidator().validate(png(1, 1, new byte[] {5, 0, 0, 0, 0}), CpmArtifactLimits.defaults());
    assertFalse(filter.success());
  }

  @Test
  void rejectsTrailingBytes() {
    var valid = png(1, 1, new byte[] {0, (byte) 255, 0, 0, (byte) 255});
    var bytes = java.util.Arrays.copyOf(valid, valid.length + 1);
    var result = new CpmPngValidator().validate(bytes, CpmArtifactLimits.defaults());
    assertFalse(result.success());
  }

  private static byte[] png(int width, int height, byte[] raw) {
    return pngWithColorType(width, height, 6, raw);
  }

  private static byte[] pngWithColorType(int width, int height, int colorType, byte[] raw) {
    try {
      var out = new ByteArrayOutputStream();
      out.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
      var ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height).put((byte) 8).put((byte) colorType).put((byte) 0).put((byte) 0).put((byte) 0).array();
      chunk(out, "IHDR", ihdr);
      var deflater = new Deflater(); deflater.setInput(raw); deflater.finish(); var compressed = new byte[256]; int length = deflater.deflate(compressed); deflater.end();
      chunk(out, "IDAT", java.util.Arrays.copyOf(compressed, length));
      chunk(out, "IEND", new byte[0]);
      return out.toByteArray();
    } catch (Exception e) { throw new AssertionError(e); }
  }

  private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
    out.write(ByteBuffer.allocate(4).putInt(data.length).array());
    byte[] name = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII); out.write(name); out.write(data);
    var crc = new CRC32(); crc.update(name); crc.update(data); out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
  }
}
