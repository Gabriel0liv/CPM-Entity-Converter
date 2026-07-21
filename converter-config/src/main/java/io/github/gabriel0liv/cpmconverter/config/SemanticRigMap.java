package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    bones = orderedCopy(bones);
    clips = orderedCopy(clips);
    stateMappings = orderedCopy(stateMappings);
    ignore = List.copyOf(ignore);
  }

  private static <K, V> Map<K, V> orderedCopy(Map<K, V> values) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
  }
}
