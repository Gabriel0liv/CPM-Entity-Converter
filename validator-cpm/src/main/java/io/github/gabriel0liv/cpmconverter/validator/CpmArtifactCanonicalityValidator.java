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
      ObjectNode canonical = (ObjectNode) (config ? canonicalConfig(parsed) : canonicalAnimation(parsed));
      byte[] expected = (JSON.writeValueAsString(canonical) + "\n").getBytes(StandardCharsets.UTF_8);
      return Arrays.equals(bytes, expected);
    } catch (Exception ex) {
      return false;
    }
  }
  private static JsonNode canonicalConfig(JsonNode n) { var o = JSON.createObjectNode(); put(o, n, "version", Kind.GENERIC); put(o, n, "skinType", Kind.GENERIC); if (n.has("skinSize")) o.set("skinSize", canonicalVector(n.get("skinSize"))); if (n.has("textures")) o.set("textures", canonicalTextures(n.get("textures"))); if (n.has("elements")) { var a = JSON.createArrayNode(); n.get("elements").forEach(v -> a.add(canonicalRoot(v))); o.set("elements", a); } copyUnknown(o, n); return o; }
  private static JsonNode canonicalTextures(JsonNode n) { var o = JSON.createObjectNode(); if (n.has("skin")) { var s = JSON.createObjectNode(); var skin = n.get("skin"); put(s, skin, "customGridSize", Kind.GENERIC); put(s, skin, "anim", Kind.GENERIC); copyUnknown(s, skin); o.set("skin", s); } copyUnknown(o, n); return o; }
  private static JsonNode canonicalAnimation(JsonNode n) { var o = JSON.createObjectNode(); put(o,n,"additive",Kind.GENERIC); put(o,n,"duration",Kind.GENERIC); if(n.has("frames")){var a=JSON.createArrayNode(); n.get("frames").forEach(v->a.add(canonicalFrame(v))); o.set("frames",a);} put(o,n,"interpolator",Kind.GENERIC); put(o,n,"loop",Kind.GENERIC); put(o,n,"name",Kind.GENERIC); put(o,n,"priority",Kind.GENERIC); copyUnknown(o,n); return o; }
  private static JsonNode canonicalFrame(JsonNode n){var o=JSON.createObjectNode(); if(n.has("components")){var a=JSON.createArrayNode();n.get("components").forEach(v->a.add(canonicalComponent(v)));o.set("components",a);} copyUnknown(o,n); return o;}
  private static JsonNode canonicalComponent(JsonNode n){var o=JSON.createObjectNode(); put(o,n,"color",Kind.GENERIC); if(n.has("pos"))o.set("pos",canonicalVector(n.get("pos"))); if(n.has("rotation"))o.set("rotation",canonicalVector(n.get("rotation"))); if(n.has("scale"))o.set("scale",canonicalVector(n.get("scale"))); put(o,n,"show",Kind.GENERIC); put(o,n,"storeID",Kind.GENERIC); copyUnknown(o,n); return o;}
  private static JsonNode canonicalRoot(JsonNode n){var o=JSON.createObjectNode(); for(String f:ROOT_FIELDS) if(n.has(f)) o.set(f, f.equals("pos")||f.equals("rotation")?canonicalVector(n.get(f)):f.equals("children")?canonicalChildren(n.get(f)):n.get(f)); copyUnknown(o,n); return o;}
  private static JsonNode canonicalChildren(JsonNode n){var a=JSON.createArrayNode(); n.forEach(v->a.add(v.isObject()&&v.has("storeID")?canonicalElement(v):canonicalRoot(v))); return a;}
  private static JsonNode canonicalElement(JsonNode n){var o=JSON.createObjectNode(); for(String f:ELEMENT_FIELDS) if(n.has(f)){JsonNode v=n.get(f); if(List.of("offset","pos","rotation","size","rscale","scale").contains(f))v=canonicalVector(v); else if(f.equals("faceUV"))v=canonicalFaceUv(v); else if(f.equals("children"))v=canonicalChildren(v); o.set(f,v);} copyUnknown(o,n); return o;}
  private static JsonNode canonicalFaceUv(JsonNode n){var o=JSON.createObjectNode(); for(String f:List.of("north","south","east","west","up","down"))if(n.has(f)){var face=JSON.createObjectNode();for(String k:List.of("sx","sy","ex","ey","rot","autoUV"))put(face,n.get(f),k,Kind.GENERIC);o.set(f,face);}copyUnknown(o,n);return o;}
  private static JsonNode canonicalVector(JsonNode n){var o=JSON.createObjectNode();put(o,n,"x",Kind.GENERIC);put(o,n,"y",Kind.GENERIC);put(o,n,"z",Kind.GENERIC);copyUnknown(o,n);return o;}
  private enum Kind { GENERIC }
  private static void put(ObjectNode out, JsonNode in, String field, Kind ignored){if(in.has(field))out.set(field,in.get(field));}
  private static void copyUnknown(ObjectNode out, JsonNode in){in.fieldNames().forEachRemaining(f->{if(!out.has(f))out.set(f,in.get(f));});}
  private static final List<String> ROOT_FIELDS=List.of("id","show","showInEditor","locked","pos","rotation","dup","disableVanillaAnim","name","nameColor","children");
  private static final List<String> ELEMENT_FIELDS=List.of("name","show","texture","textureSize","offset","pos","rotation","size","rscale","scale","u","v","faceUV","color","mirror","mcScale","glow","recolor","hidden","singleTex","extrude","locked","nameColor","storeID","children");
  private static final List<String> CONFIG_FIELDS = List.of("version", "skinType", "skinSize", "textures", "elements");
  private static final List<String> ANIMATION_FIELDS = List.of("additive", "duration", "frames", "interpolator", "loop", "name", "priority");
  private static Diagnostic warning(String source, String message) { return warning(source, message, "reason", message, "observed"); }
  private static Diagnostic warning(String source, String message, String key, String expected, String observed) { var context = new TreeMap<String, String>(); context.put("canonicalReason", key); context.put("expected", expected); context.put("observed", observed); return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_NON_CANONICAL), new SourceLocation(new SourcePath(source), null, null, "/", null), message, "regenerate with the canonical writer", null, null, context); }
}

