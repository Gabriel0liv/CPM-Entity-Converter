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
    String previous = null;
    for (var entry : inventory.entries()) {
      if (previous != null && entry.name().compareTo(previous) < 0) {
        bag = bag.add(warning(entry.name(), "ZIP entries are not in deterministic order"));
        break;
      }
      previous = entry.name();
    }
    return bag;
  }
  private static boolean isUtf8(byte[] bytes) { try { var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); decoder.decode(ByteBuffer.wrap(bytes)); return true; } catch (CharacterCodingException ex) { return false; } }
  private static boolean hasCanonicalLineEnding(byte[] bytes) { return bytes.length > 0 && bytes[bytes.length - 1] == '\n' && new String(bytes, StandardCharsets.UTF_8).indexOf('\r') < 0; }
  private static Diagnostic warning(String source, String message) { return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_NON_CANONICAL), new SourceLocation(new SourcePath(source), null, null, "/", null), message, "regenerate with the canonical writer", null, null, new TreeMap<>()); }
}
