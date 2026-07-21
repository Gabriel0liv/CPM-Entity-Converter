package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.LinkedHashMap;
import java.util.Optional;

/** Resolves every name-bearing mapping field to stable IR IDs. */
public final class MappingCompiler {
  public Result<SemanticRigMap> compile(MappingDocumentV1 document, ModelIndex index) {
    if (document == null || index == null) {
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.CONFIG_SCHEMA_INVALID),
              "mapping document and model index are required"));
    }
    DiagnosticBag diagnostics = new DiagnosticBag();
    var bones = new LinkedHashMap<String, BoneId>();
    for (var entry : document.bones().entrySet()) {
      var result = index.bone(entry.getValue());
      diagnostics = diagnostics.addAll(result.diagnostics());
      if (result.success()) bones.put(entry.getKey(), result.value());
    }
    var clips = new LinkedHashMap<String, ClipId>();
    for (var entry : document.clips().entrySet()) {
      var result = index.clip(entry.getValue());
      diagnostics = diagnostics.addAll(result.diagnostics());
      if (result.success()) clips.put(entry.getKey(), result.value());
    }
    var rootRoles = new LinkedHashMap<String, BoneId>();
    for (var entry : document.rootRoles().entrySet()) {
      var result = index.bone(entry.getValue());
      diagnostics = diagnostics.addAll(result.diagnostics());
      if (result.success()) rootRoles.put(entry.getKey(), result.value());
    }

    CompiledLookConfig look = null;
    if (document.look() != null) {
      var source = document.look();
      Optional<BoneId> head = Optional.empty();
      Optional<BoneId> neck = Optional.empty();
      if (source.head() != null) {
        var result = index.bone(source.head());
        diagnostics = diagnostics.addAll(result.diagnostics());
        if (result.success()) head = Optional.of(result.value());
      }
      if (source.neck() != null) {
        var result = index.bone(source.neck());
        diagnostics = diagnostics.addAll(result.diagnostics());
        if (result.success()) neck = Optional.of(result.value());
      }
      look =
          new CompiledLookConfig(
              head,
              neck,
              source.composition(),
              source.neckInfluence() == null ? 0 : source.neckInfluence(),
              source.headInfluence() == null ? 1 : source.headInfluence(),
              Boolean.TRUE.equals(source.allowOverrotation()));
    }

    var states = new LinkedHashMap<String, CompiledStateMapping>();
    for (var entry : document.stateMappings().entrySet()) {
      var state = entry.getValue();
      var result = index.clip(state.clip());
      if (!result.success() && Boolean.TRUE.equals(state.optional())) {
        diagnostics =
            diagnostics.add(
                Diagnostic.of(
                    Severity.INFO,
                    DiagnosticCode.fromCatalog(DiagnosticCodes.ANIM_OPTIONAL_CLIP_MISSING),
                    state.clip()));
        continue;
      }
      diagnostics = diagnostics.addAll(result.diagnostics());
      if (result.success())
        states.put(
            entry.getKey(),
            new CompiledStateMapping(
                result.value(),
                state.mode(),
                Boolean.TRUE.equals(state.optional()),
                state.requestedFps()));
    }
    if (diagnostics.hasErrors()) return Result.failure(diagnostics);
    Integer fps = document.sampling() == null ? 20 : document.sampling().requestedFps();
    var ignores =
        document.ignore().stream().map(value -> new CompiledIgnoreRule(value, "user")).toList();
    var policy =
        document.diagnosticPolicy() == null
            ? null
            : new CompiledDiagnosticPolicy(
                Boolean.TRUE.equals(document.diagnosticPolicy().warningsAsErrors()),
                Boolean.TRUE.equals(document.diagnosticPolicy().ignoreUnsupported()));
    return Result.success(
        SemanticRigMap.create(
            bones,
            clips,
            new CompiledRootRoles(rootRoles),
            look,
            states,
            fps == null ? null : new CompiledSamplingPolicy(fps),
            ignores,
            policy,
            document.modelScale(),
            document.verticalOffset(),
            document.skin(),
            document.rootStrategy()),
        diagnostics);
  }
}
