package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

/** Strict, single-pass materialization of a persisted static project. */
final class CpmPersistedProjectParser {
  private static final Set<String> ROOT_IDS = Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
  private static final Set<String> ROOT_FIELDS = Set.of("id", "customPart", "show", "showInEditor", "locked", "children", "pos", "rotation", "dup", "disableVanillaAnim", "name", "nameColor", "storeID");
  private static final Set<String> ELEMENT_FIELDS = Set.of("name", "show", "texture", "textureSize", "offset", "pos", "rotation", "size", "rscale", "scale", "u", "v", "color", "mirror", "mcScale", "glow", "recolor", "hidden", "singleTex", "extrude", "locked", "nameColor", "faceUV", "children", "storeID", "itemRenderer", "copyTransform");

  Result<CpmPersistedProjectV1> parse(JsonNode config, CpmValidatedConfigV1 validated, CpmPngMetadata png, boolean texturePresent, CpmArtifactLimits limits) {
    var diagnostics = new ParseDiagnostics();
    if (config == null || !config.isObject()) { diagnostics.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, "/", "config must be an object")); return Result.failure(diagnostics.toBag()); }
    JsonNode rootsNode = config.get("elements");
    if (rootsNode == null || !rootsNode.isArray()) { diagnostics.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, "/elements", "elements must be an array")); return Result.failure(diagnostics.toBag()); }
    var roots = new ArrayList<CpmPersistedRootV1>(); var elements = new ArrayList<CpmPersistedElementV1>(); var generated = new LinkedHashMap<Long, CpmPersistedElementV1>(); var targets = new LinkedHashMap<Long, CpmPersistedTargetV1>(); var rootIds = new HashSet<String>(); var vanillaIds = new HashSet<String>(); int[] preorder = {0};
    for (int r = 0; r < rootsNode.size(); r++) {
      String pointer = "/elements/" + r; JsonNode root = rootsNode.get(r);
      if (!root.isObject()) { diagnostics.add(error(DiagnosticCodes.CPM_INVALID_ROOT, pointer, "root must be an object")); continue; }
      unknown(root, ROOT_FIELDS, pointer, diagnostics);
      String id = requiredText(root, "id", pointer + "/id", diagnostics);
      boolean customPart = optionalBoolean(root, "customPart", false, pointer + "/customPart", diagnostics);
      boolean duplicate = optionalBoolean(root, "dup", false, pointer + "/dup", diagnostics);
      Long persistedStoreId = optionalStoreId(root, pointer + "/storeID", diagnostics);
      CpmPersistedRootKind kind;
      if (customPart) kind = CpmPersistedRootKind.CUSTOM;
      else if (duplicate) kind = CpmPersistedRootKind.DUPLICATE;
      else kind = CpmPersistedRootKind.VANILLA;
      if (id == null || (kind == CpmPersistedRootKind.VANILLA && !ROOT_IDS.contains(id))) { diagnostics.add(error(DiagnosticCodes.CPM_INVALID_ROOT, pointer + "/id", "unknown root id")); continue; }
      if (kind == CpmPersistedRootKind.VANILLA && !vanillaIds.add(id)) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_ROOT, pointer + "/id", "duplicate vanilla root id"));
      if (kind != CpmPersistedRootKind.VANILLA && persistedStoreId == null) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, pointer + "/storeID", "storeID required for duplicate or custom root"));
      if (kind == CpmPersistedRootKind.VANILLA && persistedStoreId != null) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, pointer + "/storeID", "vanilla root must not declare storeID"));
      JsonNode childrenNode = root.get("children"); if (childrenNode != null && !childrenNode.isArray()) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_ROOT, pointer + "/children", "children must be an array"));
      var children = new ArrayList<CpmPersistedElementV1>();
      if (childrenNode == null || childrenNode.isArray()) if (childrenNode != null) for (int i = 0; i < childrenNode.size(); i++) { var child = parseElement(childrenNode.get(i), pointer + "/children/" + i, 0, preorder, elements, generated, diagnostics, limits); if (child != null) children.add(child); }
      long effectiveId = kind == CpmPersistedRootKind.VANILLA ? rootId(id) : (persistedStoreId == null ? 0 : persistedStoreId);
      Long constructorStoreId = kind == CpmPersistedRootKind.VANILLA ? null : (persistedStoreId == null ? 7L : persistedStoreId);
      CpmPersistedRootV1 rootValue = new CpmPersistedRootV1(id, kind, customPart, duplicate, constructorStoreId, effectiveId, optionalBoolean(root, "show", false, pointer + "/show", diagnostics), optionalBoolean(root, "showInEditor", true, pointer + "/showInEditor", diagnostics), optionalBoolean(root, "locked", false, pointer + "/locked", diagnostics), strictVec(root, "pos", pointer + "/pos", new CpmPersistedVec3(0, 0, 0), diagnostics), strictVec(root, "rotation", pointer + "/rotation", new CpmPersistedVec3(0, 0, 0), diagnostics), optionalBoolean(root, "disableVanillaAnim", false, pointer + "/disableVanillaAnim", diagnostics), optionalText(root, "name", "", pointer + "/name", diagnostics), optionalInt(root, "nameColor", 0, pointer + "/nameColor", diagnostics), children, pointer);
      roots.add(rootValue);
      long rootTargetId = rootValue.effectiveStoreId();
      if (targets.containsKey(rootTargetId)) {
        diagnostics.add(error(DiagnosticCodes.CPM_INVALID_ROOT, pointer + "/id", "duplicate effective root target"));
      } else {
        targets.put(rootTargetId, new CpmPersistedRootTargetV1(rootValue));
      }
    }
    if (diagnostics.hasErrors()) return Result.failure(diagnostics.toBag());
    for (var e : elements) { if (e.storeId() <= 6 || e.storeId() > 9_007_199_254_740_991L || targets.containsKey(e.storeId())) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, e.pointer() + "/storeID", "invalid or colliding storeID")); else { generated.put(e.storeId(), e); targets.put(e.storeId(), new CpmPersistedElementTargetV1(e)); } }
    if (diagnostics.hasErrors()) return Result.failure(diagnostics.toBag());
    CpmPersistedTextureV1 texture = texturePresent && png != null ? new CpmPersistedTextureV1("skin.png", validated.skinSize(), validated.customGridSize(), png) : null;
    return Result.success(new CpmPersistedProjectV1(1, validated.skinType(), validated.skinSize(), roots, elements, generated, targets, texture), diagnostics.toBag());
  }

  private CpmPersistedElementV1 parseElement(JsonNode node, String pointer, int depth, int[] preorder, List<CpmPersistedElementV1> elements, Map<Long,CpmPersistedElementV1> generated, ParseDiagnostics diagnostics, CpmArtifactLimits limits) {
    if (!node.isObject()) { diagnostics.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, pointer, "element must be an object")); return null; }
    if (elements.size() >= limits.maxElements()) { diagnostics.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, pointer, "element limit exceeded")); return null; }
    if (depth > limits.maxElementDepth()) { diagnostics.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, pointer, "element depth limit exceeded")); return null; }
    unknown(node, ELEMENT_FIELDS, pointer, diagnostics);
    if (node.has("itemRenderer")) diagnostics.add(error(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED, pointer + "/itemRenderer", "itemRenderer is outside the supported profile"));
    if (node.has("copyTransform")) diagnostics.add(error(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED, pointer + "/copyTransform", "copyTransform is outside the supported profile"));
    long storeId = requiredStoreId(node, pointer + "/storeID", diagnostics);
    int index = preorder[0]++;
    while (elements.size() <= index) {
      elements.add(null);
    }
    var children = new ArrayList<CpmPersistedElementV1>(); JsonNode childrenNode = node.get("children");
    if (childrenNode != null && !childrenNode.isArray()) diagnostics.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, pointer + "/children", "children must be an array"));
    if (childrenNode != null && childrenNode.isArray()) for (int i = 0; i < childrenNode.size(); i++) { var child = parseElement(childrenNode.get(i), pointer + "/children/" + i, depth + 1, preorder, elements, generated, diagnostics, limits); if (child != null) children.add(child); }
    CpmPersistedElementV1 value = new CpmPersistedElementV1(optionalText(node, "name", "", pointer + "/name", diagnostics), optionalBoolean(node, "show", false, pointer + "/show", diagnostics), optionalBoolean(node, "texture", false, pointer + "/texture", diagnostics), optionalInt(node, "textureSize", 1, pointer + "/textureSize", diagnostics), strictVec(node, "offset", pointer + "/offset", new CpmPersistedVec3(0, 0, 0), diagnostics), strictVec(node, "pos", pointer + "/pos", new CpmPersistedVec3(0, 0, 0), diagnostics), strictVec(node, "rotation", pointer + "/rotation", new CpmPersistedVec3(0, 0, 0), diagnostics), strictVec(node, "size", pointer + "/size", new CpmPersistedVec3(0, 0, 0), diagnostics), strictVec(node, "rscale", pointer + "/rscale", new CpmPersistedVec3(1, 1, 1), diagnostics), strictVec(node, "scale", pointer + "/scale", new CpmPersistedVec3(1, 1, 1), diagnostics), parseUv(node, pointer, diagnostics), optionalText(node, "color", "000000", pointer + "/color", diagnostics), optionalBoolean(node, "mirror", false, pointer + "/mirror", diagnostics), optionalDouble(node, "mcScale", 1, pointer + "/mcScale", diagnostics), optionalBoolean(node, "glow", false, pointer + "/glow", diagnostics), optionalBoolean(node, "recolor", false, pointer + "/recolor", diagnostics), optionalBoolean(node, "hidden", false, pointer + "/hidden", diagnostics), optionalBoolean(node, "singleTex", false, pointer + "/singleTex", diagnostics), optionalBoolean(node, "extrude", false, pointer + "/extrude", diagnostics), optionalBoolean(node, "locked", false, pointer + "/locked", diagnostics), optionalInt(node, "nameColor", 0, pointer + "/nameColor", diagnostics), storeId, children, index, depth, pointer);
    elements.set(index, value);
    return value;
  }

  private CpmPersistedUvV1 parseUv(JsonNode node, String pointer, ParseDiagnostics diagnostics) { if (!node.has("faceUV")) return new CpmPersistedBoxUvV1(optionalInt(node, "u", 0, pointer + "/u", diagnostics), optionalInt(node, "v", 0, pointer + "/v", diagnostics)); JsonNode faces = node.get("faceUV"); if (!faces.isObject() || faces.isEmpty()) { diagnostics.add(error(DiagnosticCodes.UV_INVALID, pointer + "/faceUV", "faceUV must be a non-empty object")); return new CpmPersistedBoxUvV1(0, 0); } var map = new LinkedHashMap<CpmPersistedFace,CpmPersistedFaceUvV1>(); for (var it = faces.fields(); it.hasNext();) { var entry = it.next(); CpmPersistedFace face; try { face = CpmPersistedFace.valueOf(entry.getKey().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { diagnostics.add(error(DiagnosticCodes.UV_FACE_UNKNOWN, pointer + "/faceUV/" + entry.getKey(), "unknown face")); continue; } JsonNode f = entry.getValue(); if (!f.isObject()) { diagnostics.add(error(DiagnosticCodes.UV_INVALID, pointer + "/faceUV/" + entry.getKey(), "face must be object")); continue; } unknown(f, Set.of("sx", "sy", "ex", "ey", "rot", "autoUV"), pointer + "/faceUV/" + entry.getKey(), diagnostics); int sx = optionalInt(f, "sx", 0, pointer + "/faceUV/" + entry.getKey() + "/sx", diagnostics); int sy = optionalInt(f, "sy", 0, pointer + "/faceUV/" + entry.getKey() + "/sy", diagnostics); int ex = optionalInt(f, "ex", 0, pointer + "/faceUV/" + entry.getKey() + "/ex", diagnostics); int ey = optionalInt(f, "ey", 0, pointer + "/faceUV/" + entry.getKey() + "/ey", diagnostics); String rot = optionalText(f, "rot", "0", pointer + "/faceUV/" + entry.getKey() + "/rot", diagnostics); CpmPersistedUvRotation rotation = switch (rot) { case "90" -> CpmPersistedUvRotation.ROT_90; case "180" -> CpmPersistedUvRotation.ROT_180; case "270" -> CpmPersistedUvRotation.ROT_270; case "0" -> CpmPersistedUvRotation.ROT_0; default -> { diagnostics.add(error(DiagnosticCodes.UV_INVALID, pointer + "/faceUV/" + entry.getKey() + "/rot", "invalid rotation")); yield CpmPersistedUvRotation.ROT_0; } }; map.put(face, new CpmPersistedFaceUvV1(sx, sy, ex, ey, rotation, optionalBoolean(f, "autoUV", false, pointer + "/faceUV/" + entry.getKey() + "/autoUV", diagnostics))); } return new CpmPersistedPerFaceUvV1(map); }

  private static void unknown(JsonNode node, Set<String> allowed, String pointer, ParseDiagnostics d) { var it = node.fieldNames(); while (it.hasNext()) { String f = it.next(); if (!allowed.contains(f)) d.add(error(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED, pointer + "/" + f, "unsupported field")); } }
  private static String requiredText(JsonNode n, String f, String p, ParseDiagnostics d) { if (!n.has(f) || !n.get(f).isTextual() || n.get(f).textValue().isBlank()) { d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, p, "text required")); return null; } return n.get(f).textValue(); }
  private static String optionalText(JsonNode n,String f,String def,String p,ParseDiagnostics d){if(!n.has(f))return def;if(!n.get(f).isTextual()){d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p,"text required"));return def;}return n.get(f).textValue();}
  private static boolean optionalBoolean(JsonNode n,String f,boolean def,String p,ParseDiagnostics d){if(!n.has(f))return def;if(!n.get(f).isBoolean()){d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p,"boolean required"));return def;}return n.get(f).booleanValue();}
  private static int optionalInt(JsonNode n,String f,int def,String p,ParseDiagnostics d){if(!n.has(f))return def;if(!n.get(f).isIntegralNumber()||!n.get(f).canConvertToInt()){d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p,"integer required"));return def;}return n.get(f).intValue();}
  private static long requiredStoreId(JsonNode n,String p,ParseDiagnostics d){if(!n.has("storeID")||!n.get("storeID").isIntegralNumber()||!n.get("storeID").canConvertToLong()){d.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID,p,"safe storeID required"));return 0;}return n.get("storeID").longValue();}
  private static Long optionalStoreId(JsonNode n,String p,ParseDiagnostics d){if(!n.has("storeID"))return null;JsonNode v=n.get("storeID");if(!v.isIntegralNumber()||!v.canConvertToLong()){d.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID,p,"safe storeID required"));return null;}long id=v.longValue();if(id<0||id>9_007_199_254_740_991L){d.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID,p,"storeID outside safe range"));return null;}if(id<=6)d.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID,p,"reserved storeID"));return id;}
  private static double optionalDouble(JsonNode n,String f,double def,String p,ParseDiagnostics d){if(!n.has(f))return def;if(!n.get(f).isNumber()||!Double.isFinite(n.get(f).doubleValue())){d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p,"finite number required"));return def;}return n.get(f).doubleValue();}
  private static CpmPersistedVec3 strictVec(JsonNode n,String f,String p,CpmPersistedVec3 def,ParseDiagnostics d){if(!n.has(f))return def;JsonNode v=n.get(f);if(!v.isObject()){d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p,"vector object required"));return def;}var names=new HashSet<String>();v.fieldNames().forEachRemaining(names::add);if(!names.equals(Set.of("x","y","z"))){for(String x:names)if(!Set.of("x","y","z").contains(x))d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p+"/"+x,"unknown vector axis"));for(String x:Set.of("x","y","z"))if(!v.has(x))d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p+"/"+x,"missing vector axis"));return def;}double x=v.get("x").isNumber()?v.get("x").doubleValue():Double.NaN,y=v.get("y").isNumber()?v.get("y").doubleValue():Double.NaN,z=v.get("z").isNumber()?v.get("z").doubleValue():Double.NaN;if(!Double.isFinite(x))d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p+"/x","finite number required"));if(!Double.isFinite(y))d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p+"/y","finite number required"));if(!Double.isFinite(z))d.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,p+"/z","finite number required"));return new CpmPersistedVec3(Double.isFinite(x)?x:0,Double.isFinite(y)?y:0,Double.isFinite(z)?z:0);}
  private static int rootId(String id){return switch(id){case "head"->0;case "body"->1;case "left_arm"->2;case "right_arm"->3;case "left_leg"->4;case "right_leg"->5;default->-1;};}
  private static Diagnostic error(String code,String pointer,String message){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath("config.json"),null,null,pointer,null),message,"repair persisted project",null,null,new TreeMap<>());}
  private static final class ParseDiagnostics { private final List<Diagnostic> values=new ArrayList<>(); void add(Diagnostic d){values.add(d);} boolean hasErrors(){return values.stream().anyMatch(d->d.severity()==Severity.ERROR);} DiagnosticBag toBag(){return new DiagnosticBag(values);} }
}
