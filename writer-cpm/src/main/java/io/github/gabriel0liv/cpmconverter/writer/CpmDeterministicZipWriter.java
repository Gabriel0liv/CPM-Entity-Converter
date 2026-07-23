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
    return normalizeZipExtraTimestamps(out.toByteArray());
  }

  private byte[] normalizeZipExtraTimestamps(byte[] bytes) {
    // Walk only the headers emitted by this writer; compressed data is never scanned.
    int offset = 0;
    while (readInt(bytes, offset) == 0x04034b50) {
      int nameLength = readShort(bytes, offset + 26);
      int extraLength = readShort(bytes, offset + 28);
      normalizeExtraFields(bytes, offset + 30 + nameLength, extraLength);
      int compressedSize = readInt(bytes, offset + 18);
      offset += 30 + nameLength + extraLength + compressedSize;
    }
    while (readInt(bytes, offset) == 0x02014b50) {
      int nameLength = readShort(bytes, offset + 28);
      int extraLength = readShort(bytes, offset + 30);
      int commentLength = readShort(bytes, offset + 32);
      normalizeExtraFields(bytes, offset + 46 + nameLength, extraLength);
      offset += 46 + nameLength + extraLength + commentLength;
    }
    return bytes;
  }

  private void normalizeExtraFields(byte[] bytes, int start, int length) {
    byte[] fixed = {0x00, (byte) 0xA6, (byte) 0xCE, 0x12};
    int end = start + length;
    for (int i = start; i + 4 <= end;) {
      int id = readShort(bytes, i); int size = readShort(bytes, i + 2);
      if (size < 0 || i + 4 + size > end) return;
      if (id == 0x5455 && size >= 5 && (bytes[i + 4] & 1) != 0) System.arraycopy(fixed, 0, bytes, i + 5, fixed.length);
      i += 4 + size;
    }
  }

  private static int readShort(byte[] bytes, int offset) {
    return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
  }

  private static int readInt(byte[] bytes, int offset) {
    if (offset + 4 > bytes.length) return -1;
    return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8)
        | ((bytes[offset + 2] & 0xff) << 16) | ((bytes[offset + 3] & 0xff) << 24);
  }
  private void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    if (name.isBlank() || name.contains("\\") || name.startsWith("/") || name.contains("..")) throw new IOException("invalid entry");
    var e = new ZipEntry(name); e.setTimeLocal(FIXED_ENTRY_TIME);
    // Keep the explicit local-time contract while pinning the epoch used by the JDK's
    // DOS/extended timestamp encoder, which otherwise depends on the process timezone.
    e.setTime(FIXED_ENTRY_TIME.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    e.setExtra(null); e.setMethod(ZipEntry.DEFLATED); e.setComment(null); zip.putNextEntry(e); zip.write(bytes); zip.closeEntry();
  }
}
