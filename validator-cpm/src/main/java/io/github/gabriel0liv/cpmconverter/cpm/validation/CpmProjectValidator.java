package io.github.gabriel0liv.cpmconverter.cpm.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Headless validator for existing and converter-generated CPM V1 project archives. */
public final class CpmProjectValidator {
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
  private static final BigInteger MAX_SAFE_STORE_ID = BigInteger.valueOf(9_007_199_254_740_991L);
  private static final LocalDateTime GENERATED_ZIP_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);
  private static final Set<String> VANILLA_ROOT_IDS =
      Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
  private static final List<String> GENERATED_ROOT_ORDER =
      List.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
  private static final Set<String> FACE_NAMES =
      Set.of("up", "down", "north", "south", "east", "west");
  private static final Set<String> FACE_ROTATIONS = Set.of("0", "90", "180", "270");
  private static final Set<String> GENERATED_INTERPOLATORS =
      Set.of(
          "poly_loop",
          "poly_single",
          "linear_loop",
          "linear_single",
          "no_interpolate",
          "trig_loop",
          "trig_single");
  private static final Set<String> VANILLA_POSE_PREFIXES =
      Set.of(
          "custom",
          "standing",
          "walking",
          "running",
          "sneaking",
          "swimming",
          "falling",
          "sleeping",
          "riding",
          "flying",
          "dying",
          "skull_render",
          "global",
          "creative_flying",
          "eating_left",
          "eating_right",
          "retro_swimming",
          "jumping",
          "sneak_walk",
          "punch_left",
          "punch_right",
          "armor_head",
          "armor_body",
          "armor_legs",
          "armor_boots",
          "wearing_elytra",
          "bow_left",
          "bow_right",
          "crossbow_left",
          "crossbow_right",
          "crossbow_ch_left",
          "crossbow_ch_right",
          "trident_left",
          "trident_right",
          "trident_spin",
          "spyglass_left",
          "spyglass_right",
          "holding_left",
          "holding_right",
          "wearing_skull",
          "blocking_left",
          "blocking_right",
          "parrot_left",
          "parrot_right",
          "hurt",
          "on_fire",
          "freezing",
          "on_ladder",
          "climbing_on_ladder",
          "speaking",
          "toot_horn_left",
          "toot_horn_right",
          "in_gui",
          "first_person_mod",
          "voice_muted",
          "vr_first_person",
          "vr_third_person_sitting",
          "vr_third_person_standing",
          "first_person_hand",
          "health",
          "hunger",
          "air",
          "in_menu",
          "invisible",
          "light",
          "head_rotation_yaw",
          "head_rotation_pitch",
          "brush_left",
          "brush_right",
          "crawling",
          "spear_left",
          "spear_right");

  public Result<CpmValidationReport> validate(byte[] archive, CpmValidationProfile profile) {
    if (archive == null || !hasZipSignature(archive)) {
      return failure(DiagnosticCodes.CPM_ZIP_INVALID, "artifact is not a readable ZIP container");
    }
    if (profile == null) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "validation profile is required");
    }

    LinkedHashMap<String, byte[]> entries;
    try {
      entries = readEntries(archive);
    } catch (IOException | IllegalArgumentException exception) {
      String detail = exception.getMessage();
      return failure(
          DiagnosticCodes.CPM_ZIP_INVALID,
          detail == null
              ? "artifact ZIP cannot be read"
              : "artifact ZIP cannot be read: " + detail);
    }

    byte[] configBytes = entries.get("config.json");
    if (configBytes == null) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json is required");
    }

    JsonNode config;
    try {
      config = JSON.readTree(configBytes);
    } catch (IOException exception) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json is not valid JSON");
    }
    if (config == null || !config.isObject()) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json must contain a JSON object");
    }

    JsonNode version = config.get("version");
    if (version == null || !version.canConvertToInt() || !version.isIntegralNumber()) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json version must be an integer");
    }
    if (version.intValue() != 1) {
      return failure(
          DiagnosticCodes.INPUT_UNSUPPORTED_VERSION,
          "unsupported CPM project version " + version.asText() + "; expected 1");
    }

    JsonNode elements = config.get("elements");
    if (elements == null || !elements.isArray()) {
      return failure(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json elements must be an array");
    }

    Set<Long> persistedStoreIds = new HashSet<>();
    Diagnostic graphDiagnostic = validateGraph(elements, profile, persistedStoreIds);
    if (graphDiagnostic != null) return Result.failure(graphDiagnostic);

    Diagnostic animationDiagnostic = validateAnimations(entries, persistedStoreIds, profile);
    if (animationDiagnostic != null) return Result.failure(animationDiagnostic);

    if (profile == CpmValidationProfile.GENERATED_V1) {
      Diagnostic deterministicDiagnostic = validateGeneratedArchive(archive, entries);
      if (deterministicDiagnostic != null) return Result.failure(deterministicDiagnostic);
    }

    int elementCount = countElements(elements);
    int storeIdCount = countStoreIds(elements);
    int animationCount =
        (int)
            entries.keySet().stream()
                .filter(name -> name.startsWith("animations/") && name.endsWith(".json"))
                .count();
    return Result.success(
        new CpmValidationReport(entries.size(), elementCount, animationCount, storeIdCount));
  }

  private Diagnostic validateGraph(
      JsonNode roots, CpmValidationProfile profile, Set<Long> seenStoreIds) {
    Set<String> seenVanillaRoots = new HashSet<>();
    StoreIdCursor generatedIds = new StoreIdCursor();
    int generatedRootIndex = 0;

    for (JsonNode root : roots) {
      if (!root.isObject()) {
        return error(DiagnosticCodes.CPM_CONFIG_INVALID, "each CPM root must be an object");
      }

      boolean customPart = root.path("customPart").asBoolean(false);
      boolean duplicated = root.path("dup").asBoolean(false);
      JsonNode idNode = root.get("id");
      if (idNode == null || !idNode.isTextual() || idNode.textValue().isBlank()) {
        return error(DiagnosticCodes.CPM_INVALID_ROOT, "CPM root id must be a non-empty string");
      }
      String rawId = idNode.textValue();
      String id = rawId.toLowerCase(Locale.ROOT);
      if (!customPart && !duplicated) {
        if (!VANILLA_ROOT_IDS.contains(id)) {
          return error(DiagnosticCodes.CPM_INVALID_ROOT, "unknown vanilla CPM root: " + id);
        }
        if (!seenVanillaRoots.add(id)) {
          return error(DiagnosticCodes.CPM_INVALID_ROOT, "duplicate vanilla CPM root: " + id);
        }
      }

      if (profile == CpmValidationProfile.GENERATED_V1) {
        if (customPart || duplicated || !rawId.equals(id)) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "converter-generated CPM roots must use lowercase canonical vanilla roots");
        }
        if (generatedRootIndex >= GENERATED_ROOT_ORDER.size()
            || !id.equals(GENERATED_ROOT_ORDER.get(generatedRootIndex))) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "converter-generated CPM roots are not in canonical order");
        }
        generatedRootIndex++;
      }

      if (root.has("storeID")) {
        Diagnostic storeIdDiagnostic =
            validateStoreId(root.get("storeID"), seenStoreIds, null, "root " + id);
        if (storeIdDiagnostic != null) return storeIdDiagnostic;
        if (profile == CpmValidationProfile.GENERATED_V1) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "converter-generated vanilla roots must not persist storeID fields");
        }
      }

      JsonNode children = root.get("children");
      if (children != null) {
        if (!children.isArray()) {
          return error(DiagnosticCodes.CPM_CONFIG_INVALID, "root children must be an array");
        }
        Diagnostic childDiagnostic =
            validateChildren(children, profile, seenStoreIds, generatedIds);
        if (childDiagnostic != null) return childDiagnostic;
      }
    }

    if (profile == CpmValidationProfile.GENERATED_V1
        && generatedRootIndex != GENERATED_ROOT_ORDER.size()) {
      return error(
          DiagnosticCodes.CPM_VALIDATION_FAILED,
          "converter-generated CPM must contain all six canonical vanilla roots");
    }
    return null;
  }

  private Diagnostic validateChildren(
      JsonNode children,
      CpmValidationProfile profile,
      Set<Long> seenStoreIds,
      StoreIdCursor generatedIds) {
    for (JsonNode child : children) {
      if (!child.isObject()) {
        return error(DiagnosticCodes.CPM_CONFIG_INVALID, "each CPM child must be an object");
      }
      JsonNode storeId = child.get("storeID");
      if (storeId == null) {
        return error(DiagnosticCodes.CPM_CONFIG_INVALID, "CPM child storeID is required");
      }
      Long expected = profile == CpmValidationProfile.GENERATED_V1 ? generatedIds.take() : null;
      Diagnostic storeIdDiagnostic =
          validateStoreId(storeId, seenStoreIds, expected, "child element");
      if (storeIdDiagnostic != null) return storeIdDiagnostic;

      Diagnostic uvDiagnostic = validateUv(child);
      if (uvDiagnostic != null) return uvDiagnostic;

      JsonNode nested = child.get("children");
      if (nested != null) {
        if (!nested.isArray()) {
          return error(DiagnosticCodes.CPM_CONFIG_INVALID, "child children must be an array");
        }
        Diagnostic nestedDiagnostic = validateChildren(nested, profile, seenStoreIds, generatedIds);
        if (nestedDiagnostic != null) return nestedDiagnostic;
      }
    }
    return null;
  }

  private Diagnostic validateStoreId(
      JsonNode node, Set<Long> seenStoreIds, Long expected, String context) {
    if (!node.isIntegralNumber()) {
      return error(DiagnosticCodes.CPM_STORE_ID_RANGE, context + " storeID must be an integer");
    }
    BigInteger value = node.bigIntegerValue();
    if (value.signum() <= 0 || value.compareTo(MAX_SAFE_STORE_ID) > 0) {
      return error(
          DiagnosticCodes.CPM_STORE_ID_RANGE, context + " storeID must be positive and <= 2^53-1");
    }
    long storeId = value.longValueExact();
    if (!seenStoreIds.add(storeId)) {
      return error(DiagnosticCodes.CPM_DUPLICATE_STORE_ID, "duplicate CPM storeID " + storeId);
    }
    if (expected != null && storeId != expected.longValue()) {
      return error(
          DiagnosticCodes.CPM_VALIDATION_FAILED,
          "generated CPM storeID preorder expected " + expected + " but found " + storeId);
    }
    return null;
  }

  private Diagnostic validateUv(JsonNode child) {
    Diagnostic uDiagnostic = validateOptionalUvInt(child.get("u"), "u");
    if (uDiagnostic != null) return uDiagnostic;
    Diagnostic vDiagnostic = validateOptionalUvInt(child.get("v"), "v");
    if (vDiagnostic != null) return vDiagnostic;

    JsonNode faceUv = child.get("faceUV");
    if (faceUv == null) return null;
    if (!faceUv.isObject()) {
      return error(DiagnosticCodes.CPM_UV_INVALID, "faceUV must be an object");
    }
    var fields = faceUv.fields();
    while (fields.hasNext()) {
      var entry = fields.next();
      if (!FACE_NAMES.contains(entry.getKey())) {
        return error(DiagnosticCodes.CPM_UV_INVALID, "unknown CPM faceUV face " + entry.getKey());
      }
      JsonNode face = entry.getValue();
      if (!face.isObject()) {
        return error(DiagnosticCodes.CPM_UV_INVALID, "faceUV face must be an object");
      }
      for (String coordinate : new String[] {"sx", "sy", "ex", "ey"}) {
        JsonNode coordinateNode = face.get(coordinate);
        if (coordinateNode == null) {
          return error(
              DiagnosticCodes.CPM_UV_INVALID,
              "faceUV " + entry.getKey() + " requires " + coordinate);
        }
        Diagnostic coordinateDiagnostic =
            validateUvInt(coordinateNode, entry.getKey() + "." + coordinate);
        if (coordinateDiagnostic != null) return coordinateDiagnostic;
      }
      JsonNode rotation = face.get("rot");
      if (rotation != null
          && (!rotation.isTextual() || !FACE_ROTATIONS.contains(rotation.textValue()))) {
        return error(DiagnosticCodes.CPM_UV_INVALID, "invalid faceUV rotation");
      }
    }
    return null;
  }

  private Diagnostic validateOptionalUvInt(JsonNode node, String field) {
    if (node == null) return null;
    return validateUvInt(node, field);
  }

  private Diagnostic validateUvInt(JsonNode node, String field) {
    if (!node.isIntegralNumber() || !node.canConvertToInt()) {
      return error(
          DiagnosticCodes.CPM_UV_INVALID, field + " must be exactly representable as CPM V1 int");
    }
    return null;
  }

  private Diagnostic validateAnimations(
      LinkedHashMap<String, byte[]> entries,
      Set<Long> persistedStoreIds,
      CpmValidationProfile profile) {
    for (var entry : entries.entrySet()) {
      if (!isAnimationJsonEntry(entry.getKey())) continue;
      if (!isRecognizedAnimationEntry(entry.getKey())) {
        if (profile == CpmValidationProfile.GENERATED_V1) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "converter-generated animation filename is not recognized by CPM V1: "
                  + entry.getKey());
        }
        continue;
      }

      JsonNode animation;
      try {
        animation = JSON.readTree(entry.getValue());
      } catch (IOException exception) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID,
            "animation " + entry.getKey() + " is not valid JSON");
      }
      if (animation == null || !animation.isObject()) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID,
            "animation " + entry.getKey() + " must contain a JSON object");
      }

      Diagnostic headerDiagnostic = validateAnimationHeader(animation, profile, entry.getKey());
      if (headerDiagnostic != null) return headerDiagnostic;

      JsonNode frames = animation.get("frames");
      if (frames == null || !frames.isArray()) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID,
            "animation " + entry.getKey() + " requires a frames array");
      }

      for (JsonNode frame : frames) {
        if (!frame.isObject()) {
          return error(DiagnosticCodes.CPM_FRAME_INVALID, "animation frame must be an object");
        }
        JsonNode components = frame.get("components");
        if (components == null || !components.isArray()) {
          return error(
              DiagnosticCodes.CPM_FRAME_INVALID, "animation frame requires a components array");
        }
        for (JsonNode component : components) {
          Diagnostic componentDiagnostic =
              validateAnimationComponent(component, persistedStoreIds, profile);
          if (componentDiagnostic != null) return componentDiagnostic;
        }
      }
    }
    return null;
  }

  private Diagnostic validateAnimationHeader(
      JsonNode animation, CpmValidationProfile profile, String entryName) {
    JsonNode additive = animation.get("additive");
    if (additive == null || !additive.isBoolean()) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID,
          "animation " + entryName + " requires boolean additive");
    }

    JsonNode duration = animation.get("duration");
    if (duration == null
        || !duration.isIntegralNumber()
        || !duration.canConvertToInt()
        || duration.intValue() <= 0) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID,
          "animation " + entryName + " duration must be a positive integer");
    }

    JsonNode priority = animation.get("priority");
    if (priority != null && (!priority.isIntegralNumber() || !priority.canConvertToInt())) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID,
          "animation " + entryName + " priority must be an integer");
    }

    JsonNode loop = animation.get("loop");
    if (loop != null && !loop.isBoolean()) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID, "animation " + entryName + " loop must be boolean");
    }

    JsonNode interpolator = animation.get("interpolator");
    if (interpolator != null && !interpolator.isTextual()) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID,
          "animation " + entryName + " interpolator must be a string");
    }

    if (profile == CpmValidationProfile.GENERATED_V1) {
      if (loop == null || interpolator == null) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID,
            "generated animation requires loop and interpolator fields");
      }
      String rawInterpolator = interpolator.textValue();
      String value = rawInterpolator.toLowerCase(Locale.ROOT);
      if (!rawInterpolator.equals(value) || !GENERATED_INTERPOLATORS.contains(value)) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID,
            "generated animation uses noncanonical interpolator " + rawInterpolator);
      }
      if (!value.equals("no_interpolate")) {
        boolean loopInterpolator = value.endsWith("_loop");
        if (loop.booleanValue() != loopInterpolator) {
          return error(
              DiagnosticCodes.CPM_FRAME_INVALID,
              "generated animation loop/interpolator combination is inconsistent");
        }
      }
    }
    return null;
  }

  private Diagnostic validateAnimationComponent(
      JsonNode component, Set<Long> persistedStoreIds, CpmValidationProfile profile) {
    if (!component.isObject()) {
      return error(DiagnosticCodes.CPM_FRAME_INVALID, "animation component must be an object");
    }

    JsonNode storeIdNode = component.get("storeID");
    if (storeIdNode == null || !storeIdNode.isIntegralNumber()) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID, "animation component storeID must be an integer");
    }
    BigInteger storeIdValue = storeIdNode.bigIntegerValue();
    if (storeIdValue.signum() < 0 || storeIdValue.compareTo(MAX_SAFE_STORE_ID) > 0) {
      return error(
          DiagnosticCodes.CPM_FRAME_INVALID,
          "animation component storeID must be between 0 and 2^53-1");
    }
    long storeId = storeIdValue.longValueExact();
    if (!isReservedVanillaStoreId(storeId) && !persistedStoreIds.contains(storeId)) {
      return error(
          DiagnosticCodes.CPM_DANGLING_ANIMATION_REF,
          "animation component references missing CPM storeID " + storeId);
    }

    for (String transform : new String[] {"pos", "rotation", "scale"}) {
      JsonNode vector = component.get(transform);
      if (vector == null) {
        if (profile == CpmValidationProfile.GENERATED_V1) {
          return error(
              DiagnosticCodes.CPM_FRAME_INVALID,
              "generated animation component requires " + transform);
        }
        continue;
      }
      Diagnostic vectorDiagnostic = validateAnimationVector(vector, transform);
      if (vectorDiagnostic != null) return vectorDiagnostic;
    }

    JsonNode color = component.get("color");
    if (color == null || !color.isTextual() || !isCpmColor(color.textValue())) {
      return error(DiagnosticCodes.CPM_FRAME_INVALID, "animation component has invalid color");
    }
    JsonNode show = component.get("show");
    if (show == null || !show.isBoolean()) {
      return error(DiagnosticCodes.CPM_FRAME_INVALID, "animation component show must be boolean");
    }
    return null;
  }

  private Diagnostic validateAnimationVector(JsonNode vector, String field) {
    if (!vector.isObject()) {
      return error(DiagnosticCodes.CPM_FRAME_INVALID, field + " transform must be an object");
    }
    for (String axis : new String[] {"x", "y", "z"}) {
      JsonNode value = vector.get(axis);
      if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
        return error(
            DiagnosticCodes.CPM_FRAME_INVALID, field + "." + axis + " must be a finite number");
      }
    }
    return null;
  }

  private boolean isCpmColor(String value) {
    if (value == null || value.isEmpty()) return false;
    try {
      Integer.parseUnsignedInt(value, 16);
      return true;
    } catch (NumberFormatException exception) {
      return false;
    }
  }

  private boolean isReservedVanillaStoreId(long storeId) {
    return storeId >= 0 && storeId <= 6;
  }

  private boolean isAnimationJsonEntry(String name) {
    if (name == null || !name.startsWith("animations/") || !name.endsWith(".json")) return false;
    String fileName = name.substring("animations/".length());
    return !fileName.isEmpty() && fileName.indexOf('/') < 0;
  }

  private boolean isRecognizedAnimationEntry(String name) {
    if (!isAnimationJsonEntry(name)) return false;
    String fileName = name.substring("animations/".length());
    if (fileName.startsWith("g_") || fileName.startsWith("c_")) return true;
    if (!fileName.startsWith("v_")) return false;
    String poseName = fileName.substring(2, fileName.length() - ".json".length());
    return VANILLA_POSE_PREFIXES.stream().anyMatch(poseName::startsWith);
  }

  private Diagnostic validateGeneratedArchive(
      byte[] archive, LinkedHashMap<String, byte[]> entries) {
    List<String> actualOrder = new ArrayList<>();
    try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        actualOrder.add(entry.getName());
        if (entry.isDirectory()) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "generated CPM archives must not contain directory entries");
        }
        if (entry.getMethod() != ZipEntry.STORED) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "generated CPM ZIP entries must use STORED method");
        }
        if (!GENERATED_ZIP_TIME.equals(entry.getTimeLocal())) {
          return error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "generated CPM ZIP entries must use the fixed 1980-01-01 timestamp");
        }
      }
    } catch (IOException exception) {
      return error(
          DiagnosticCodes.CPM_VALIDATION_FAILED,
          "generated CPM ZIP metadata could not be validated");
    }

    List<String> lexicalOrder = new ArrayList<>(actualOrder);
    lexicalOrder.sort(String::compareTo);
    if (!actualOrder.equals(lexicalOrder)) {
      return error(
          DiagnosticCodes.CPM_VALIDATION_FAILED,
          "generated CPM ZIP entries must be lexicographically ordered");
    }

    for (var entry : entries.entrySet()) {
      if (!entry.getKey().endsWith(".json")) continue;
      JsonNode json;
      try {
        json = JSON.readTree(entry.getValue());
      } catch (IOException exception) {
        return error(
            DiagnosticCodes.CPM_VALIDATION_FAILED,
            "generated JSON entry is invalid: " + entry.getKey());
      }
      byte[] canonical;
      try {
        canonical = canonicalJson(json);
      } catch (IllegalArgumentException exception) {
        return error(
            DiagnosticCodes.CPM_VALIDATION_FAILED,
            "generated JSON entry cannot be canonicalized: " + entry.getKey());
      }
      if (json == null || !Arrays.equals(entry.getValue(), canonical)) {
        return error(
            DiagnosticCodes.CPM_VALIDATION_FAILED,
            "generated JSON entry is not canonical: " + entry.getKey());
      }
    }
    return null;
  }

  private byte[] canonicalJson(JsonNode value) {
    if (value == null) throw new IllegalArgumentException("generated JSON root is null");
    StringBuilder builder = new StringBuilder();
    appendCanonicalJson(builder, value);
    builder.append('\n');
    return builder.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void appendCanonicalJson(StringBuilder builder, JsonNode value) {
    if (value == null || value.isNull()) {
      builder.append("null");
    } else if (value.isTextual()) {
      appendCanonicalString(builder, value.textValue());
    } else if (value.isBoolean()) {
      builder.append(value.booleanValue());
    } else if (value.isIntegralNumber()) {
      builder.append(value.bigIntegerValue());
    } else if (value.isNumber()) {
      double number = value.doubleValue();
      if (!Double.isFinite(number)) {
        throw new IllegalArgumentException("non-finite generated JSON number");
      }
      builder.append(Double.toString(number));
    } else if (value.isObject()) {
      builder.append('{');
      List<String> names = new ArrayList<>();
      value.fieldNames().forEachRemaining(names::add);
      names.sort(String::compareTo);
      boolean first = true;
      for (String name : names) {
        if (!first) builder.append(',');
        first = false;
        appendCanonicalString(builder, name);
        builder.append(':');
        appendCanonicalJson(builder, value.get(name));
      }
      builder.append('}');
    } else if (value.isArray()) {
      builder.append('[');
      boolean first = true;
      for (JsonNode child : value) {
        if (!first) builder.append(',');
        first = false;
        appendCanonicalJson(builder, child);
      }
      builder.append(']');
    } else {
      throw new IllegalArgumentException("unsupported generated JSON node");
    }
  }

  private void appendCanonicalString(StringBuilder builder, String value) {
    builder.append('"');
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\b' -> builder.append("\\b");
        case '\f' -> builder.append("\\f");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (character < 0x20) {
            builder.append(String.format("\\u%04x", (int) character));
          } else {
            builder.append(character);
          }
        }
      }
    }
    builder.append('"');
  }

  private LinkedHashMap<String, byte[]> readEntries(byte[] archive) throws IOException {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    Set<String> normalizedNames = new HashSet<>();
    try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        String name = entry.getName();
        if (unsafeEntryName(name)) {
          throw new IllegalArgumentException("unsafe ZIP entry path: " + name);
        }
        String normalizedName = name.toLowerCase(Locale.ROOT);
        if (!normalizedNames.add(normalizedName)) {
          throw new IllegalArgumentException("duplicate or case-colliding ZIP entry: " + name);
        }
        entries.put(name, input.readAllBytes());
      }
    }
    return entries;
  }

  private boolean unsafeEntryName(String name) {
    if (name == null || name.isBlank()) return true;
    String normalized = name.replace('\\', '/');
    if (normalized.startsWith("/")
        || (normalized.length() >= 3
            && Character.isLetter(normalized.charAt(0))
            && normalized.charAt(1) == ':'
            && normalized.charAt(2) == '/')) {
      return true;
    }
    for (String segment : normalized.split("/", -1)) {
      if (segment.equals("..")) return true;
    }
    return false;
  }

  private int countElements(JsonNode elements) {
    int count = 0;
    for (JsonNode element : elements) {
      if (!element.isObject()) continue;
      count++;
      JsonNode children = element.get("children");
      if (children != null && children.isArray()) count += countElements(children);
    }
    return count;
  }

  private int countStoreIds(JsonNode elements) {
    int count = 0;
    for (JsonNode element : elements) {
      if (!element.isObject()) continue;
      if (element.has("storeID")) count++;
      JsonNode children = element.get("children");
      if (children != null && children.isArray()) count += countStoreIds(children);
    }
    return count;
  }

  private boolean hasZipSignature(byte[] archive) {
    if (archive.length < 4 || archive[0] != 'P' || archive[1] != 'K') return false;
    int third = archive[2] & 0xff;
    int fourth = archive[3] & 0xff;
    return (third == 3 && fourth == 4)
        || (third == 5 && fourth == 6)
        || (third == 7 && fourth == 8);
  }

  private Diagnostic error(String code, String message) {
    return Diagnostic.of(Severity.ERROR, code, message);
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(error(code, message));
  }

  private static final class StoreIdCursor {
    private long next = 1000;

    private long take() {
      return next++;
    }
  }
}
