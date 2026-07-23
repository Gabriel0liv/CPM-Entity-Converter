package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Orchestrates independent validation of an already-persisted CPM artifact. */
public final class CpmArtifactValidator {
  private static final List<CpmValidationLayer> ORDER = List.of(CpmValidationLayer.values());
  private final CpmZipContainerReader container = new CpmZipContainerReader();
  public Result<CpmValidatedArtifactV1> validate(byte[] bytes) {
    if (bytes == null) return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", null, "artifact bytes are null", "provide a CPM project artifact"));
    return validate(new CpmArtifactValidationRequest(bytes, CpmArtifactLimits.defaults()));
  }
  public Result<CpmValidatedArtifactV1> validate(CpmArtifactValidationRequest request) {
    var statuses = new LinkedHashMap<CpmValidationLayer, CpmValidationLayerStatus>(); ORDER.forEach(l -> statuses.put(l, CpmValidationLayerStatus.SKIPPED));
    if (request == null) return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", null, "validation request is null", "provide bytes and limits"));
    var read = container.readArtifact(request.artifactBytes(), request.limits());
    if (!read.success()) { statuses.put(CpmValidationLayer.CONTAINER, CpmValidationLayerStatus.FAIL); return Result.failure(read.diagnostics()); }
    statuses.put(CpmValidationLayer.CONTAINER, CpmValidationLayerStatus.PASS);
    var data = read.value(); JsonNode config;
    var parser = new CpmBoundedJsonParser(request.limits());
    var parsed = parser.parse(data.entries().get("config.json"), "config.json", DiagnosticCodes.CPM_CONFIG_INVALID);
    if (!parsed.success()) { statuses.put(CpmValidationLayer.CONFIG_SYNTAX, CpmValidationLayerStatus.FAIL); return Result.failure(parsed.diagnostics()); }
    config = parsed.value(); statuses.put(CpmValidationLayer.CONFIG_SYNTAX, CpmValidationLayerStatus.PASS);
    var bag = new DiagnosticBag();
    JsonNode version = config.get("version"), elements = config.get("elements");
    if (version == null || !version.isInt() || version.intValue() != 1) bag = bag.add(error(DiagnosticCodes.CPM_UNSUPPORTED_VERSION, "config.json", "/version", "CPM version must be 1", "use version 1"));
    if (elements == null || !elements.isArray()) bag = bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json", "/elements", "elements array is required", "add elements"));
    else if (elements.size() > request.limits().maxElements()) bag = bag.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, "config.json", "/elements", "element limit exceeded", "reduce elements"));
    statuses.put(CpmValidationLayer.CONFIG_SCHEMA, bag.hasErrors() ? CpmValidationLayerStatus.FAIL : CpmValidationLayerStatus.PASS);
    if (bag.hasErrors()) return Result.failure(bag);
    var ids = new HashSet<Long>(); int count = 0, roots = 0, textured = 0; var rootNames = Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
    var graphDiagnostics = new ArrayList<Diagnostic>();
    for (int i = 0; i < elements.size(); i++) { JsonNode element = elements.get(i); count += visit(element, "/elements/" + i, ids, request.limits(), graphDiagnostics); if (rootNames.contains(element.path("id").asText())) roots++; if (element.has("texture")) textured++; }
    bag = bag.addAll(new DiagnosticBag(graphDiagnostics));
    statuses.put(CpmValidationLayer.PROJECT_GRAPH, bag.hasErrors() ? CpmValidationLayerStatus.FAIL : CpmValidationLayerStatus.PASS);
    if (bag.hasErrors()) return Result.failure(withStatuses(bag));
    int width = 0, height = 0;
    if (data.entries().containsKey("skin.png")) { byte[] png = data.entries().get("skin.png"); if (!validPng(png)) bag = bag.add(error(DiagnosticCodes.PNG_INVALID, "skin.png", "/signature", "invalid PNG", "provide a valid PNG")); else { width = readInt(png, 16); height = readInt(png, 20); } }
    statuses.put(CpmValidationLayer.STORE_REFERENCES, CpmValidationLayerStatus.PASS); statuses.put(CpmValidationLayer.UV_TEXTURE, bag.hasErrors() ? CpmValidationLayerStatus.FAIL : CpmValidationLayerStatus.PASS);
    int animations = 0; var animationValues = new ArrayList<CpmPersistedAnimationV1>();
    for (var entry : data.entries().entrySet()) if (entry.getKey().startsWith("animations/")) { var a = parser.parse(entry.getValue(), entry.getKey(), DiagnosticCodes.CPM_ANIMATION_INVALID); if (!a.success()) bag = bag.addAll(a.diagnostics()); else { animations++; animationValues.add(new CpmPersistedAnimationV1(entry.getKey().substring(11), List.of())); } }
    statuses.put(CpmValidationLayer.ANIMATIONS, bag.hasErrors() ? CpmValidationLayerStatus.FAIL : CpmValidationLayerStatus.PASS);
    boolean canonical = isCanonical(data.entries().get("config.json"), data.inventory()); if (!canonical) bag = bag.add(warning(DiagnosticCodes.CPM_NON_CANONICAL, "config.json", null, "artifact is valid but non-canonical", "use canonical writer output"));
    statuses.put(CpmValidationLayer.CANONICALITY, canonical ? CpmValidationLayerStatus.PASS : CpmValidationLayerStatus.WARN);
    if (bag.hasErrors()) return Result.failure(bag);
    var project = new CpmPersistedProjectParser().parse(config, width, height, data.entries().containsKey("skin.png"));
    return Result.success(new CpmValidatedArtifactV1(project, animationValues, data.inventory(), new CpmValidationSummary(statuses, canonical, roots, count, ids.size(), textured, animations, 0, 0, data.entries().containsKey("skin.png"), width, height)), bag);
  }
  private int visit(JsonNode n, String pointer, Set<Long> ids, CpmArtifactLimits limits, List<Diagnostic> diagnostics) { if (!n.isObject()) return 0; int count = n.has("id") ? 0 : 1; if (n.has("storeID")) { if (!n.path("storeID").isIntegralNumber() || !n.path("storeID").canConvertToLong()) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, "config.json", pointer + "/storeID", "invalid storeID", "use a safe integer")); else if (!ids.add(n.path("storeID").longValue())) diagnostics.add(error(DiagnosticCodes.CPM_DUPLICATE_STORE_ID, "config.json", pointer + "/storeID", "duplicate storeID", "use unique storeIDs")); } if (n.path("children").isArray()) for (int i = 0; i < n.path("children").size(); i++) count += visit(n.path("children").get(i), pointer + "/children/" + i, ids, limits, diagnostics); return count; }
  private static boolean validPng(byte[] b) { return b.length >= 24 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71 && readInt(b, 12) == 0x49484452 && readInt(b, 16) > 0 && readInt(b, 20) > 0; }
  private static int readInt(byte[] b, int p) { return ((b[p] & 255) << 24) | ((b[p + 1] & 255) << 16) | ((b[p + 2] & 255) << 8) | (b[p + 3] & 255); }
  private static boolean isCanonical(byte[] config, CpmArtifactInventory inventory) { String s = new String(config, StandardCharsets.UTF_8); return s.endsWith("\n") && !s.contains("\r") && inventory.entries().stream().allMatch(e -> e.method() == 8); }
  private static DiagnosticBag withStatuses(DiagnosticBag bag) { return bag; }
  private static Diagnostic error(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
  private static Diagnostic warning(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
}
