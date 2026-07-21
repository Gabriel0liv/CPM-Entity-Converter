package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, fully resolved mapping. The only construction boundary is the package-private factory
 * used by {@link MappingCompiler}; consumers cannot create a partially compiled map or perform name
 * lookups after compilation.
 */
public final class SemanticRigMap {
  private final Map<String, BoneId> bones;
  private final Map<String, ClipId> clips;
  private final CompiledRootRoles rootRoles;
  private final CompiledLookConfig look;
  private final Map<String, CompiledStateMapping> stateMappings;
  private final CompiledSamplingPolicy sampling;
  private final List<CompiledIgnoreRule> ignore;
  private final CompiledDiagnosticPolicy diagnosticPolicy;
  private final Double modelScale;
  private final Double verticalOffset;
  private final String skin;
  private final String rootStrategy;

  private SemanticRigMap(
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
    this.bones = orderedCopy(bones);
    this.clips = orderedCopy(clips);
    this.rootRoles = rootRoles;
    this.look = look;
    this.stateMappings = orderedCopy(stateMappings);
    this.sampling = sampling;
    this.ignore = List.copyOf(ignore == null ? List.of() : ignore);
    this.diagnosticPolicy = diagnosticPolicy;
    this.modelScale = modelScale;
    this.verticalOffset = verticalOffset;
    this.skin = skin;
    this.rootStrategy = rootStrategy;
  }

  static SemanticRigMap create(
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
    return new SemanticRigMap(
        bones,
        clips,
        rootRoles,
        look,
        stateMappings,
        sampling,
        ignore,
        diagnosticPolicy,
        modelScale,
        verticalOffset,
        skin,
        rootStrategy);
  }

  public Map<String, BoneId> bones() {
    return bones;
  }

  public Map<String, ClipId> clips() {
    return clips;
  }

  public CompiledRootRoles rootRoles() {
    return rootRoles;
  }

  public CompiledLookConfig look() {
    return look;
  }

  public Map<String, CompiledStateMapping> stateMappings() {
    return stateMappings;
  }

  public CompiledSamplingPolicy sampling() {
    return sampling;
  }

  public List<CompiledIgnoreRule> ignore() {
    return ignore;
  }

  public CompiledDiagnosticPolicy diagnosticPolicy() {
    return diagnosticPolicy;
  }

  public Double modelScale() {
    return modelScale;
  }

  public Double verticalOffset() {
    return verticalOffset;
  }

  public String skin() {
    return skin;
  }

  public String rootStrategy() {
    return rootStrategy;
  }

  private static <K, V> Map<K, V> orderedCopy(Map<K, V> values) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof SemanticRigMap that)) return false;
    return Objects.equals(bones, that.bones)
        && Objects.equals(clips, that.clips)
        && Objects.equals(rootRoles, that.rootRoles)
        && Objects.equals(look, that.look)
        && Objects.equals(stateMappings, that.stateMappings)
        && Objects.equals(sampling, that.sampling)
        && Objects.equals(ignore, that.ignore)
        && Objects.equals(diagnosticPolicy, that.diagnosticPolicy)
        && Objects.equals(modelScale, that.modelScale)
        && Objects.equals(verticalOffset, that.verticalOffset)
        && Objects.equals(skin, that.skin)
        && Objects.equals(rootStrategy, that.rootStrategy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        bones,
        clips,
        rootRoles,
        look,
        stateMappings,
        sampling,
        ignore,
        diagnosticPolicy,
        modelScale,
        verticalOffset,
        skin,
        rootStrategy);
  }
}
