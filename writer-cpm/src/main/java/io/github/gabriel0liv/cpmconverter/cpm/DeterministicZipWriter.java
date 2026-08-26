package io.github.gabriel0liv.cpmconverter.cpm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes byte-stable ZIP archives without platform-dependent compression. */
final class DeterministicZipWriter {
  private static final LocalDateTime FIXED_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);

  private DeterministicZipWriter() {}

  static byte[] write(Map<String, byte[]> sourceEntries) throws IOException {
    List<Map.Entry<String, byte[]>> entries = new ArrayList<>(sourceEntries.entrySet());
    entries.sort(Comparator.comparing(Map.Entry::getKey));

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream archive = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> sourceEntry : entries) {
        byte[] payload = sourceEntry.getValue();
        CRC32 crc = new CRC32();
        crc.update(payload);

        ZipEntry entry = new ZipEntry(sourceEntry.getKey());
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(payload.length);
        entry.setCompressedSize(payload.length);
        entry.setCrc(crc.getValue());
        entry.setTimeLocal(FIXED_TIME);
        entry.setExtra(new byte[0]);
        archive.putNextEntry(entry);
        archive.write(payload);
        archive.closeEntry();
      }
      archive.finish();
    }
    return bytes.toByteArray();
  }
}
