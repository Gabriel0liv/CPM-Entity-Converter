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
    int width = 0, height = 0;
    DiagnosticBag bag = configResult.diagnostics();
    CpmPngMetadata pngMetadata = null;
    if (data.entries().containsKey("skin.png")) {
      var png = new CpmPngValidator().validate(data.entries().get("skin.png"), request.limits());
      if (!png.success()) bag = bag.addAll(png.diagnostics()); else { pngMetadata = png.value(); width = png.value().width(); height = png.value().height(); }
    }
    var parsedProject = new CpmPersistedProjectParser().parse(root, config, pngMetadata, data.entries().containsKey("skin.png"), request.limits());
    if (!parsedProject.success()) {
      bag = bag.addAll(parsedProject.diagnostics());
      tracker.fail(CpmValidationLayer.PROJECT_GRAPH);
      var syntaxDiagnostics = new DiagnosticBag();
      int parsedAnimationEntries = 0;
      for (var entry : data.entries().entrySet()) {
        if (!entry.getKey().startsWith("animations/")) continue;
        if (++parsedAnimationEntries > request.limits().maxAnimations()) {
          syntaxDiagnostics = syntaxDiagnostics.add(new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_LIMIT_EXCEEDED), new SourceLocation(new SourcePath(entry.getKey()), null, null, "/", null), "animation limit exceeded", "reduce animations", null, null, new TreeMap<>()));
          break;
        }
        var animation = new CpmPersistedAnimationParser().parse(entry.getKey(), entry.getValue(), request.limits());
        if (!animation.success()) syntaxDiagnostics = syntaxDiagnostics.addAll(animation.diagnostics());
      }
      bag = bag.addAll(syntaxDiagnostics);
      if (syntaxDiagnostics.hasErrors()) tracker.fail(CpmValidationLayer.ANIMATIONS);
      return Result.failure(bag);
    }
    var project = parsedProject.value();
    var graph = new CpmPersistedProjectValidator().validate(project, request.limits());
    if (graph.hasErrors()) { tracker.fail(CpmValidationLayer.PROJECT_GRAPH); return Result.failure(graph); }
    tracker.pass(CpmValidationLayer.PROJECT_GRAPH);

    var refs = new CpmPersistedStoreReferenceValidator().validate(project);
    if (refs.hasErrors()) { tracker.fail(CpmValidationLayer.STORE_REFERENCES); return Result.failure(refs); }
    // No independent static reference relation exists in the current MVP.
    // Keep STORE_REFERENCES as SKIPPED rather than reporting a fictitious PASS.

    var uv = new CpmPersistedUvTextureValidator().validate(project,
        data.entries().containsKey("skin.png"), pngMetadata, request.limits());
    bag = bag.addAll(uv);
    if (bag.hasErrors()) tracker.fail(CpmValidationLayer.UV_TEXTURE); else tracker.pass(CpmValidationLayer.UV_TEXTURE);

    var animations = new ArrayList<CpmPersistedAnimationV1>();
    var animationDiagnostics = new DiagnosticBag();
    int animationEntries = 0;
    for (var entry : data.entries().entrySet()) if (entry.getKey().startsWith("animations/")) {
      if (++animationEntries > request.limits().maxAnimations()) { animationDiagnostics = animationDiagnostics.add(new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_LIMIT_EXCEEDED), new SourceLocation(new SourcePath(entry.getKey()), null, null, "/", null), "animation limit exceeded", "reduce animations", null, null, new TreeMap<>())); break; }
      var a = new CpmPersistedAnimationParser().parse(entry.getKey(), entry.getValue(), request.limits());
      if (!a.success()) animationDiagnostics = animationDiagnostics.addAll(a.diagnostics());
      else animations.add(a.value());
    }
    animationDiagnostics = animationDiagnostics.addAll(new CpmPersistedAnimationValidator().validate(animations, project));
    bag = bag.addAll(animationDiagnostics);
    if (animationDiagnostics.hasErrors()) tracker.fail(CpmValidationLayer.ANIMATIONS); else tracker.pass(CpmValidationLayer.ANIMATIONS);
    var canonicalDiagnostics = new CpmArtifactCanonicalityValidator().validate(data.entries(), data.inventory());
    bag = bag.addAll(canonicalDiagnostics);
    boolean canonical = !canonicalDiagnostics.warnings().isEmpty() ? false : true;
    if (!canonical) tracker.warn(CpmValidationLayer.CANONICALITY); else tracker.pass(CpmValidationLayer.CANONICALITY);
    if (bag.hasErrors()) return Result.failure(bag);
    int roots = project.roots().size(), count = project.elements().size();
    int textured = (int) project.elements().stream().filter(CpmPersistedElementV1::texture).count();
    int frames = animations.stream().mapToInt(a -> a.frames().size()).sum();
    int references = animations.stream().flatMap(a -> a.frames().stream()).mapToInt(f -> f.components().size()).sum();
    return Result.success(new CpmValidatedArtifactV1(project, animations, data.inventory(), new CpmValidationSummary(tracker.snapshot(), canonical, roots, count, project.persistedTargets().size(), textured, animations.size(), frames, references, data.entries().containsKey("skin.png"), width, height)), bag);
  }
  private static Diagnostic error(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
  private static Diagnostic warning(String code, String source, String pointer, String message, String suggestion) { return new Diagnostic(Severity.WARNING, DiagnosticCode.fromCatalog(code), new SourceLocation(new SourcePath(source), null, null, pointer, null), message, suggestion, null, null, new TreeMap<>()); }
}
