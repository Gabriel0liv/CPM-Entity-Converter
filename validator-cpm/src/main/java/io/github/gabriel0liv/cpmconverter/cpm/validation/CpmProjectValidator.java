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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Headless validator for existing and converter-generated CPM V1 project archives. */
public final class CpmProjectValidator {
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

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
          detail == null ? "artifact ZIP cannot be read" : "artifact ZIP cannot be read: " + detail);
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

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }
}
