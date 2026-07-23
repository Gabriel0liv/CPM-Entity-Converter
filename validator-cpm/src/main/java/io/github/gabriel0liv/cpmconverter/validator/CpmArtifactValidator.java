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
    var tracker = new CpmValidationLayerTracker();
    if (request == null) return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", null, "validation request is null", "provide bytes and limits"));
    var read = container.readArtifact(request.artifactBytes(), request.limits());
    if (!read.success()) { tracker.fail(CpmValidationLayer.CONTAINER); return Result.failure(read.diagnostics()); }
    tracker.pass(CpmValidationLayer.CONTAINER);
    var data = read.value(); JsonNode config;
    var parser = new CpmBoundedJsonParser(request.limits());
    var parsed = parser.parse(data.entries().get("config.json"), "config.json", DiagnosticCodes.CPM_CONFIG_INVALID);
    if (!parsed.success()) { tracker.fail(CpmValidationLayer.CONFIG_SYNTAX); return Result.failure(parsed.diagnostics()); }
    config = parsed.value(); tracker.pass(CpmValidationLayer.CONFIG_SYNTAX);
    var bag = new DiagnosticBag();
    JsonNode version = config.get("version"), elements = config.get("elements");
    if (version == null || !version.isInt() || version.intValue() != 1) bag = bag.add(error(DiagnosticCodes.CPM_UNSUPPORTED_VERSION, "config.json", "/version", "CPM version must be 1", "use version 1"));
    if (elements == null || !elements.isArray()) bag = bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID, "config.json", "/elements", "elements array is required", "add elements"));
    else if (elements.size() > request.limits().maxElements()) bag = bag.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, "config.json", "/elements", "element limit exceeded", "reduce elements"));
    JsonNode skinSize=config.get("skinSize"); if(skinSize!=null){ if(!skinSize.isObject()) bag=bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,"config.json","/skinSize","skinSize must be an object","provide positive integer x/y")); else { JsonNode x=skinSize.get("x"), y=skinSize.get("y"); if(x==null||!x.isIntegralNumber()||x.intValue()<=0) bag=bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,"config.json","/skinSize/x","skinSize.x must be a positive integer","provide a positive integer")); if(y==null||!y.isIntegralNumber()||y.intValue()<=0) bag=bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,"config.json","/skinSize/y","skinSize.y must be a positive integer","provide a positive integer")); } }
    if (bag.hasErrors()) tracker.fail(CpmValidationLayer.CONFIG_SCHEMA); else tracker.pass(CpmValidationLayer.CONFIG_SCHEMA);
    if (bag.hasErrors()) return Result.failure(bag);
    var ids = new HashSet<Long>(); int count = 0, roots = 0, textured = 0; var rootNames = Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg");
    var graphDiagnostics = new ArrayList<Diagnostic>();
    for (int i = 0; i < elements.size(); i++) { JsonNode element = elements.get(i); count += visit(element, "/elements/" + i, ids, request.limits(), graphDiagnostics); if (rootNames.contains(element.path("id").asText())) roots++; if (element.has("texture")) textured++; }
    bag = bag.addAll(new DiagnosticBag(graphDiagnostics));
    if (bag.hasErrors()) tracker.fail(CpmValidationLayer.PROJECT_GRAPH); else tracker.pass(CpmValidationLayer.PROJECT_GRAPH);
    if (bag.hasErrors()) return Result.failure(withStatuses(bag));
    int width = 0, height = 0;
    if (data.entries().containsKey("skin.png")) { var pngResult = new CpmPngValidator().validate(data.entries().get("skin.png"), request.limits()); if (!pngResult.success()) bag = bag.addAll(pngResult.diagnostics()); else { width = pngResult.value().width(); height = pngResult.value().height(); } }
    tracker.pass(CpmValidationLayer.STORE_REFERENCES);
    int animations = 0; var animationValues = new ArrayList<CpmPersistedAnimationV1>();
    for (var entry : data.entries().entrySet()) if (entry.getKey().startsWith("animations/")) { var a = parser.parse(entry.getValue(), entry.getKey(), DiagnosticCodes.CPM_ANIMATION_INVALID); if (!a.success()) bag = bag.addAll(a.diagnostics()); else { animations++; animationValues.add(new CpmPersistedAnimationV1(entry.getKey().substring(11), List.of())); } }
    if (bag.hasErrors()) tracker.fail(CpmValidationLayer.ANIMATIONS); else tracker.pass(CpmValidationLayer.ANIMATIONS);
    if (bag.errors().stream().anyMatch(d -> d.code().value().startsWith("UV_") || d.code().value().startsWith("PNG_"))) tracker.fail(CpmValidationLayer.UV_TEXTURE); else tracker.pass(CpmValidationLayer.UV_TEXTURE);
    boolean canonical = isCanonical(data.entries().get("config.json"), data.inventory()); if (!canonical) { bag = bag.add(warning(DiagnosticCodes.CPM_NON_CANONICAL, "config.json", null, "artifact is valid but non-canonical", "use canonical writer output")); tracker.warn(CpmValidationLayer.CANONICALITY); } else tracker.pass(CpmValidationLayer.CANONICALITY);
    if (bag.hasErrors()) return Result.failure(bag);
    var project = new CpmPersistedProjectParser().parse(config, width, height, data.entries().containsKey("skin.png"));
    bag = bag.addAll(new CpmPersistedUvTextureValidator().validate(config, project.skinSize()));
    if (bag.hasErrors()) return Result.failure(bag);
    return Result.success(new CpmValidatedArtifactV1(project, animationValues, data.inventory(), new CpmValidationSummary(tracker.snapshot(), canonical, roots, count, ids.size(), textured, animations, 0, 0, data.entries().containsKey("skin.png"), width, height)), bag);
  }
  private int visit(JsonNode n, String pointer, Set<Long> ids, CpmArtifactLimits limits, List<Diagnostic> diagnostics) { if (!n.isObject()) return 0; int count = n.has("id") ? 0 : 1; if (n.has("storeID")) { if (!n.path("storeID").isIntegralNumber() || !n.path("storeID").canConvertToLong()) diagnostics.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, "config.json", pointer + "/storeID", "invalid storeID", "use a safe integer")); else if (!ids.add(n.path("storeID").longValue())) diagnostics.add(error(DiagnosticCodes.CPM_DUPLICATE_STORE_ID, "config.json", pointer + "/storeID", "duplicate storeID", "use unique storeIDs")); } if (n.path("children").isArray()) for (int i = 0; i < n.path("children").size(); i++) count += visit(n.path("children").get(i), pointer + "/children/" + i, ids, limits, diagnostics); return count; }
  private static boolean isCanonical(byte[] config, CpmArtifactInventory inventory) { String s = new String(config, StandardCharsets.UTF_8); return s.endsWith("\n") && !s.contains("\r") && inventory.entries().stream().allMatch(e -> e.method() == 8); }
  private static DiagnosticBag withStatuses(DiagnosticBag bag) { return bag; }
  private static Diagnostic error(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
  private static Diagnostic warning(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
}
