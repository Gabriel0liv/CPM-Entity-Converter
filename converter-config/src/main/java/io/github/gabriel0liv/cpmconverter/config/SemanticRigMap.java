package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import java.util.List;
import java.util.Map;

/** Fully resolved mapping; consumers do not perform name lookups. */
public record SemanticRigMap(
    Map<String, BoneId> bones,
    Map<String, ClipId> clips,
    CompiledRootRoles rootRoles,
    CompiledLookConfig look,
    Map<String, CompiledStateMapping> stateMappings,
    CompiledSamplingPolicy sampling,
    List<CompiledIgnoreRule> ignore,
    CompiledDiagnosticPolicy diagnosticPolicy,
    Double modelScale,
    Double verticalOffset,
    String skin,
    String rootStrategy) {
  public SemanticRigMap {
    bones = Map.copyOf(bones);
    clips = Map.copyOf(clips);
    stateMappings = Map.copyOf(stateMappings);
    ignore = List.copyOf(ignore);
  }

  /** Legacy constructor retained for tests that only exercise bone/clip resolution. */
  public SemanticRigMap(
      Map<String, BoneId> bones, Map<String, ClipId> clips, List<String> ignored) {
    this(
        bones,
        clips,
        new CompiledRootRoles(Map.of()),
        null,
        Map.of(),
        null,
        ignored.stream().map(value -> new CompiledIgnoreRule(value, "")).toList(),
        null,
        null,
        null,
        null,
        null);
  }
}
