package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;

final class CpmArtifactCanonicalityValidator {
  DiagnosticBag validate(Map<String, byte[]> entries, CpmArtifactInventory inventory) {
    var bag = new DiagnosticBag();
    byte[] config = entries.get("config.json");
    if (config == null || !isUtf8(config) || !hasCanonicalLineEnding(config)) {
      bag = bag.add(warning("config.json", "config JSON is not canonical UTF-8/LF"));
    }
    var expected = new ArrayList<String>();
    if (entries.containsKey("config.json")) expected.add("config.json");
    if (entries.containsKey("skin.png")) expected.add("skin.png");
    entries.keySet().stream().filter(n -> n.startsWith("animations/")).sorted().forEach(expected::add);
    for (int i = 0; i < inventory.entries().size(); i++) {
      var entry = inventory.entries().get(i);
      if (i >= expected.size() || !expected.get(i).equals(entry.name())) {
        bag = bag.add(warning(entry.name(), "ZIP entry order or name is not canonical", "entryOrder", i < expected.size() ? expected.get(i) : "<none>", entry.name()));
      }
      if (entry.method() != 8) bag = bag.add(warning(entry.name(), "ZIP compression method is not DEFLATED", "method", "8", Integer.toString(entry.method())));
      if (entry.localTime() != 315532800000L) bag = bag.add(warning(entry.name(), "ZIP timestamp is not the writer epoch", "timestamp", "315532800000", Long.toString(entry.localTime())));
    }
    return bag;
  }
  private static boolean isUtf8(byte[] bytes) { try { var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); decoder.decode(ByteBuffer.wrap(bytes)); return true; } catch (CharacterCodingException ex) { return false; } }
  private static boolean hasCanonicalLineEnding(byte[] bytes) { return bytes.length > 0 && bytes[bytes.length - 1] == '\n' && new String(bytes, StandardCharsets.UTF_8).indexOf('\r') < 0; }
  private static Diagnostic warning(String source, String message) { return warning(source, message, "reason", message, "observed"); }
  private static Diagnostic warning(String source, String message, String key, String expected, String observed) { var context = new TreeMap<String, String>(); context.put("canonicalReason", key); context.put("expected", expected); context.put("observed", observed); return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_NON_CANONICAL), new SourceLocation(new SourcePath(source), null, null, "/", null), message, "regenerate with the canonical writer", null, null, context); }
}
