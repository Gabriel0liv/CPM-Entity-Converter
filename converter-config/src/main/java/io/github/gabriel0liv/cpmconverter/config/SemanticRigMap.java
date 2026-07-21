package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import java.util.List;
import java.util.Map;

/** Compiled mapping containing resolved IDs and preserved user policy. */
public record SemanticRigMap(
    Map<String, BoneId> bones,
    Map<String, ClipId> clips,
    List<String> ignore,
    Double modelScale,
    Double verticalOffset,
    String skin,
    String rootStrategy,
    Map<String, String> rootRoles,
    MappingDocumentV1.Look look,
    Map<String, MappingDocumentV1.StateMapping> stateMappings,
    MappingDocumentV1.Sampling sampling,
    MappingDocumentV1.DiagnosticPolicy diagnosticPolicy) {

  public SemanticRigMap {
    bones = Map.copyOf(bones);
    clips = Map.copyOf(clips);
    ignore = List.copyOf(ignore);
    rootRoles = Map.copyOf(rootRoles == null ? Map.of() : rootRoles);
    stateMappings = Map.copyOf(stateMappings == null ? Map.of() : stateMappings);
  }

  public SemanticRigMap(Map<String, BoneId> bones, Map<String, ClipId> clips, List<String> ignore) {
    this(bones, clips, ignore, null, null, null, null, Map.of(), null, Map.of(), null, null);
  }
}
