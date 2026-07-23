package io.github.gabriel0liv.cpmconverter.writer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.time.LocalDateTime;

final class CpmDeterministicZipWriter {
  static final LocalDateTime FIXED_ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0, 0);
  byte[] write(byte[] config, byte[] skin) throws IOException {
    var out = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      zip.setLevel(9); entry(zip, "config.json", config); entry(zip, "skin.png", skin); zip.finish();
    }
    return normalizeExtendedTimestamps(out.toByteArray());
  }

  private byte[] normalizeExtendedTimestamps(byte[] bytes) {
    // ZipEntry.setTimeLocal records an extended Unix timestamp based on the
    // default zone. Normalize that optional field to the fixed UTC instant.
    byte[] fixed = {0x00, (byte) 0xA6, (byte) 0xCE, 0x12};
    for (int i = 0; i + 9 < bytes.length; i++) {
      if (bytes[i] == 0x55 && bytes[i + 1] == 0x54 && bytes[i + 2] == 0x05
          && bytes[i + 3] == 0x00 && bytes[i + 4] == 0x01) {
        System.arraycopy(fixed, 0, bytes, i + 5, fixed.length);
      }
    }
    return bytes;
  }
  private void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    if (name.isBlank() || name.contains("\\") || name.startsWith("/") || name.contains("..")) throw new IOException("invalid entry");
    var e = new ZipEntry(name); e.setTimeLocal(FIXED_ENTRY_TIME); e.setExtra(null); e.setMethod(ZipEntry.DEFLATED); e.setComment(null); zip.putNextEntry(e); zip.write(bytes); zip.closeEntry();
  }
}
