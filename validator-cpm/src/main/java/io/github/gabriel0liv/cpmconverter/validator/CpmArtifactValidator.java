package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Orchestrates the independent validation layers of a persisted CPM artifact. */
public final class CpmArtifactValidator {
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
    var data = read.value();
    var parser = new CpmBoundedJsonParser(request.limits());
    var parsed = parser.parse(data.entries().get("config.json"), "config.json", DiagnosticCodes.CPM_CONFIG_INVALID);
    if (!parsed.success()) { tracker.fail(CpmValidationLayer.CONFIG_SYNTAX); return Result.failure(parsed.diagnostics()); }
    tracker.pass(CpmValidationLayer.CONFIG_SYNTAX);

    var configResult = new CpmPersistedConfigValidator().validate(parsed.value());
    if (!configResult.success()) { tracker.fail(CpmValidationLayer.CONFIG_SCHEMA); return Result.failure(configResult.diagnostics()); }
    tracker.pass(CpmValidationLayer.CONFIG_SCHEMA);
    CpmValidatedConfigV1 config = configResult.value();
    JsonNode root = config.source();
    JsonNode elements = root.path("elements");
    var project = new CpmPersistedProjectParser().parse(root, config.skinSize(), 0, 0, data.entries().containsKey("skin.png"));
    var graph = new CpmPersistedProjectValidator().validate(project, request.limits());
    if (graph.hasErrors()) { tracker.fail(CpmValidationLayer.PROJECT_GRAPH); return Result.failure(graph); }
    tracker.pass(CpmValidationLayer.PROJECT_GRAPH);

    var refs = new CpmPersistedStoreReferenceValidator().validate(project);
    if (refs.hasErrors()) { tracker.fail(CpmValidationLayer.STORE_REFERENCES); return Result.failure(refs); }
    tracker.pass(CpmValidationLayer.STORE_REFERENCES);

    DiagnosticBag bag = configResult.diagnostics();
    int width = 0, height = 0;
    if (data.entries().containsKey("skin.png")) {
      var png = new CpmPngValidator().validate(data.entries().get("skin.png"), request.limits());
      if (!png.success()) bag = bag.addAll(png.diagnostics()); else { width = png.value().width(); height = png.value().height(); }
    }
    var uv = new CpmPersistedUvTextureValidator().validate(root, config.skinSize());
    bag = bag.addAll(uv);
    if (bag.hasErrors()) tracker.fail(CpmValidationLayer.UV_TEXTURE); else tracker.pass(CpmValidationLayer.UV_TEXTURE);

    var animations = new ArrayList<CpmPersistedAnimationV1>();
    var animationDiagnostics = new DiagnosticBag();
    for (var entry : data.entries().entrySet()) if (entry.getKey().startsWith("animations/")) {
      var a = parser.parse(entry.getValue(), entry.getKey(), DiagnosticCodes.CPM_ANIMATION_INVALID);
      if (!a.success()) animationDiagnostics = animationDiagnostics.addAll(a.diagnostics());
      else animations.add(new CpmPersistedAnimationV1(entry.getKey().substring(11), List.of()));
    }
    bag = bag.addAll(animationDiagnostics);
    if (animationDiagnostics.hasErrors()) tracker.fail(CpmValidationLayer.ANIMATIONS); else tracker.pass(CpmValidationLayer.ANIMATIONS);
    boolean canonical = isCanonical(data.entries().get("config.json"), data.inventory());
    if (!canonical) { bag = bag.add(warning(DiagnosticCodes.CPM_NON_CANONICAL, "config.json", null, "artifact is valid but non-canonical", "use canonical writer output")); tracker.warn(CpmValidationLayer.CANONICALITY); }
    else tracker.pass(CpmValidationLayer.CANONICALITY);
    if (bag.hasErrors()) return Result.failure(bag);
    var finalProject = new CpmPersistedProjectParser().parse(root, config.skinSize(), width, height, data.entries().containsKey("skin.png"));
    int roots = finalProject.roots().size(), count = finalProject.elements().size(), textured = (int) finalProject.elements().stream().filter(e -> true).count();
    return Result.success(new CpmValidatedArtifactV1(finalProject, animations, data.inventory(), new CpmValidationSummary(tracker.snapshot(), canonical, roots, count, finalProject.generatedStoreIds().size(), textured, animations.size(), 0, 0, data.entries().containsKey("skin.png"), width, height)), bag);
  }
  private static boolean isCanonical(byte[] config, CpmArtifactInventory inventory) { String s = new String(config, StandardCharsets.UTF_8); return s.endsWith("\n") && !s.contains("\r") && inventory.entries().stream().allMatch(e -> e.method() == 8); }
  private static Diagnostic error(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
  private static Diagnostic warning(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
}
