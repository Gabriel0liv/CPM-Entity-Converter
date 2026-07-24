package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class CpmZipContainerReader {
  record CpmReadArtifact(CpmArtifactInventory inventory, Map<String, byte[]> entries) {
    CpmReadArtifact { entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries)); }
  }
  public Result<CpmArtifactInventory> read(byte[] bytes, CpmArtifactLimits limits) { return readArtifact(bytes, limits).map(CpmReadArtifact::inventory); }
  Result<CpmReadArtifact> readArtifact(byte[] bytes, CpmArtifactLimits limits) {
    if (bytes == null || bytes.length == 0) return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", "empty artifact"));
    if (bytes.length > limits.maxArtifactBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, "<artifact>", "artifact size limit exceeded"));
    var metadata = new ArrayList<CpmArtifactEntry>(); var entries = new LinkedHashMap<String, byte[]>(); var names = new HashSet<String>(); long total = 0;
    try (var in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry e; int position = 0;
      while ((e = in.getNextEntry()) != null) {
        if (position >= limits.maxEntries()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, "<artifact>", "entry count limit exceeded"));
        String name = e.getName();
        if (e.isDirectory() || !safe(name, limits.maxEntryNameLength())) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_UNSAFE, name == null ? "<artifact>" : name, "unsafe entry name"));
        if (!names.add(name.toLowerCase(Locale.ROOT))) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_DUPLICATE, name, "duplicate entry"));
        if (!(e.getMethod() == ZipEntry.STORED || e.getMethod() == ZipEntry.DEFLATED)) return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, name, "unsupported compression method"));
        var out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int read; long observed = 0;
        while ((read = in.read(buffer)) != -1) { observed += read; total += read; long entryLimit=name.equals("config.json")?limits.maxConfigBytes():name.startsWith("animations/")?limits.maxAnimationJsonBytes():name.equals("skin.png")?limits.maxSkinBytes():limits.maxEntryUncompressedBytes(); if (observed > entryLimit || total > limits.maxTotalUncompressedBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, name, "uncompressed limit exceeded")); long compressed=e.getCompressedSize(); if(compressed>0 && observed > compressed * (long)limits.maxCompressionRatio()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,name,"compression ratio limit exceeded")); out.write(buffer, 0, read); }
        byte[] value = out.toByteArray(); entries.put(name, value); metadata.add(new CpmArtifactEntry(name, e.getMethod(), e.getTime(), e.getCompressedSize(), value.length, e.getCrc(), false, position++));
      }
    } catch (IOException ex) { return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", "invalid ZIP container")); }
    if (!entries.containsKey("config.json")) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_MISSING, "<artifact>", "config.json is required"));
    for (String name : entries.keySet()) if (!recognized(name)) return Result.failure(error(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED, name, "unsupported artifact entry"));
    return Result.success(new CpmReadArtifact(new CpmArtifactInventory(metadata), entries));
  }
  private static boolean recognized(String name) { return name.equals("config.json") || name.equals("skin.png") || (name.startsWith("animations/") && name.endsWith(".json") && !name.substring(11).contains("/")); }
  private static boolean safe(String name, int max) { if (name == null || name.isBlank() || name.length() > max || name.indexOf('\0') >= 0 || name.startsWith("/") || name.startsWith("\\") || name.contains("\\") || name.endsWith("/")) return false; if (name.matches("^[A-Za-z]:.*")) return false; for (String segment : name.split("/", -1)) if (segment.isEmpty() || segment.equals("..")) return false; return true; }
  private static Diagnostic error(String code, String source, String message) { return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code), SourceLocation.of(new SourcePath(source)), message, "repair the artifact", null, null, new TreeMap<>()); }
}
