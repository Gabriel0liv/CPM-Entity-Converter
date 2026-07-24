package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;

final class CpmArtifactCanonicalityValidator {
  private static final ObjectMapper JSON = new ObjectMapper();
  DiagnosticBag validate(Map<String, byte[]> entries, CpmArtifactInventory inventory) {
    var bag = new DiagnosticBag();
    byte[] config = entries.get("config.json");
    if (config == null || !isCanonicalJsonBytes(config, true)) bag = bag.add(warning("config.json", "config JSON bytes are not canonical"));
    for (var entry : entries.entrySet()) if (entry.getKey().startsWith("animations/") && !isCanonicalJsonBytes(entry.getValue(), false)) bag = bag.add(warning(entry.getKey(), "animation JSON bytes are not canonical"));
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
  private static boolean isCanonicalJsonBytes(byte[] bytes, boolean config) {
    if (!isUtf8(bytes) || !hasCanonicalLineEnding(bytes)) return false;
    int end = bytes.length - 1;
    boolean string = false, escaped = false;
    for (int i = 0; i < end; i++) {
      int c = bytes[i] & 0xff;
      if (string) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') string = false; continue; }
      if (c == '"') { string = true; continue; }
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n') return false;
    }
    if (string) return false;
    try {
      JsonNode parsed = JSON.readTree(new String(bytes, StandardCharsets.UTF_8));
      if (parsed == null || !parsed.isObject()) return false;
      ObjectNode canonical = (ObjectNode) canonicalObject(parsed, config ? CONFIG_FIELDS : ANIMATION_FIELDS);
      byte[] expected = (JSON.writeValueAsString(canonical) + "\n").getBytes(StandardCharsets.UTF_8);
      return Arrays.equals(bytes, expected);
    } catch (Exception ex) {
      return false;
    }
  }
  private static JsonNode canonicalObject(JsonNode node, List<String> rootOrder) {
    if (node.isArray()) { var a = JSON.createArrayNode(); node.forEach(v -> a.add(canonicalObject(v, rootOrder))); return a; }
    if (!node.isObject()) return node;
    var out = JSON.createObjectNode(); var order = orderFor(node, rootOrder);
    for (String field : order) if (node.has(field)) out.set(field, canonicalObject(node.get(field), rootOrder));
    node.fieldNames().forEachRemaining(field -> { if (!out.has(field)) out.set(field, canonicalObject(node.get(field), rootOrder)); });
    return out;
  }
  private static List<String> orderFor(JsonNode node, List<String> rootOrder) {
    if (node.has("customGridSize") || node.has("anim")) return List.of("customGridSize", "anim");
    if (node.has("skin")) return List.of("skin");
    if (node.has("showInEditor") || node.has("disableVanillaAnim")) return List.of("id", "show", "showInEditor", "locked", "pos", "rotation", "dup", "disableVanillaAnim", "name", "nameColor", "children");
    if (node.has("storeID") || node.has("faceUV") || node.has("textureSize")) return List.of("name", "show", "texture", "textureSize", "offset", "pos", "rotation", "size", "rscale", "scale", "u", "v", "faceUV", "color", "mirror", "mcScale", "glow", "recolor", "hidden", "singleTex", "extrude", "locked", "nameColor", "storeID", "children");
    if (node.has("sx") || node.has("rot")) return List.of("sx", "sy", "ex", "ey", "rot", "autoUV");
    if (node.has("x") || node.has("y") || node.has("z")) return List.of("x", "y", "z");
    return rootOrder;
  }
  private static final List<String> CONFIG_FIELDS = List.of("version", "skinType", "skinSize", "textures", "elements");
  private static final List<String> ANIMATION_FIELDS = List.of("additive", "duration", "frames", "interpolator", "loop", "name", "priority");
  private static Diagnostic warning(String source, String message) { return warning(source, message, "reason", message, "observed"); }
  private static Diagnostic warning(String source, String message, String key, String expected, String observed) { var context = new TreeMap<String, String>(); context.put("canonicalReason", key); context.put("expected", expected); context.put("observed", observed); return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_NON_CANONICAL), new SourceLocation(new SourcePath(source), null, null, "/", null), message, "regenerate with the canonical writer", null, null, context); }
}
