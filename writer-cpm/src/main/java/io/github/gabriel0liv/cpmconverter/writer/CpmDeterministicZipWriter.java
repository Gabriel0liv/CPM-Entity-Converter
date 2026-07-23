package io.github.gabriel0liv.cpmconverter.writer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

final class CpmDeterministicZipWriter {
  byte[] write(byte[] config, byte[] skin) throws IOException {
    var out = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      zip.setLevel(9); entry(zip, "config.json", config); entry(zip, "skin.png", skin); zip.finish();
    }
    return out.toByteArray();
  }
  private void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
    if (name.isBlank() || name.contains("\\") || name.startsWith("/") || name.contains("..")) throw new IOException("invalid entry");
    var e = new ZipEntry(name); e.setTime(315532800000L); e.setMethod(ZipEntry.DEFLATED); e.setComment(null); zip.putNextEntry(e); zip.write(bytes); zip.closeEntry();
  }
}
