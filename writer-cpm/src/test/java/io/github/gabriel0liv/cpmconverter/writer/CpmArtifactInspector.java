package io.github.gabriel0liv.cpmconverter.writer;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

final class CpmArtifactInspector {
  record InspectedEntry(String name, int method, LocalDateTime time, long uncompressedSize, long crc, byte[] contents) {
    InspectedEntry { contents = contents.clone(); }
    @Override public byte[] contents() { return contents.clone(); }
  }
  record InspectedArtifact(List<InspectedEntry> entries) {
    InspectedArtifact { entries = List.copyOf(entries); }
  }
  static InspectedArtifact inspect(byte[] bytes) throws IOException {
    var entries = new ArrayList<InspectedEntry>(); var names = new HashSet<String>();
    try (var in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (!names.add(entry.getName())) throw new IOException("duplicate entry");
        var out = new ByteArrayOutputStream(); in.transferTo(out);
        byte[] contents = out.toByteArray();
        CRC32 checksum = new CRC32(); checksum.update(contents);
        entries.add(new InspectedEntry(entry.getName(), entry.getMethod(), entry.getTimeLocal(), contents.length,
            entry.getCrc() >= 0 ? entry.getCrc() : checksum.getValue(), contents));
      }
    }
    return new InspectedArtifact(entries);
  }
}
