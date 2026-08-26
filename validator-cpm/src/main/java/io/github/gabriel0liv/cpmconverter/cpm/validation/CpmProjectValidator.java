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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Headless validator for existing and converter-generated CPM V1 project archives. */
public final class CpmProjectValidator {
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
  private static final BigInteger MAX_SAFE_STORE_ID = BigInteger.valueOf(9_007_199_254_740_991L);
  private static final Set<String> VANILLA_ROOT_IDS =
      Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
  private static final Set<String> FACE_NAMES =
      Set.of("up", "down", "north", "south", "east", "west");
  private static final Set<String> FACE_ROTATIONS = Set.of("0", "90", "180", "270");

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

    Diagnostic graphDiagnostic = validateGraph(elements, profile);
    if (graphDiagnostic != null) return Result.failure(graphDiagnostic);

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

  private Diagnostic validateGraph(JsonNode roots, CpmValidationProfile profile) {
    Set<String> seenVanillaRoots = new HashSet<>();
    Set<Long> seenStoreIds = new HashSet<>();
    StoreIdCursor generatedIds = new StoreIdCursor();

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
      String id = idNode.textValue().toLowerCase(Locale.ROOT);
      if (!customPart && !duplicated) {
        if (!VANILLA_ROOT_IDS.contains(id)) {
          return error(DiagnosticCodes.CPM_INVALID_ROOT, "unknown vanilla CPM root: " + id);
        }
        if (!seenVanillaRoots.add(id)) {
          return error(DiagnosticCodes.CPM_INVALID_ROOT, "duplicate vanilla CPM root: " + id);
        }
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
      Long expected =
          profile == CpmValidationProfile.GENERATED_V1 ? generatedIds.take() : null;
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
        Diagnostic nestedDiagnostic =
            validateChildren(nested, profile, seenStoreIds, generatedIds);
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
          DiagnosticCodes.CPM_STORE_ID_RANGE,
          context + " storeID must be positive and <= 2^53-1");
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

  private LinkedHashMap<String, byte[]> readEntries(byte[] archive) throws IOException {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    Set<String> names = new TreeSet<>();
    try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        String name = entry.getName();
        if (unsafeEntryName(name)) {
          throw new IllegalArgumentException("unsafe ZIP entry path: " + name);
        }
        if (!names.add(name)) {
          throw new IllegalArgumentException("duplicate ZIP entry: " + name);
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
