package io.github.gabriel0liv.cpmconverter.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = false)
public record MappingDocumentV1(
    @JsonProperty("schemaVersion") Integer schemaVersion,
    @JsonProperty("modelScale") Double modelScale,
    @JsonProperty("verticalOffset") Double verticalOffset,
    @JsonProperty("skin") String skin,
    @JsonProperty("rootStrategy") String rootStrategy,
    @JsonProperty("rootRoles") Map<String, String> rootRoles,
    @JsonProperty("bones") Map<String, String> bones,
    @JsonProperty("clips") Map<String, String> clips,
    @JsonProperty("look") Look look,
    @JsonProperty("stateMappings") Map<String, StateMapping> stateMappings,
    @JsonProperty("sampling") Sampling sampling,
    @JsonProperty("ignore") List<String> ignore,
    @JsonProperty("diagnosticPolicy") DiagnosticPolicy diagnosticPolicy) {

  public MappingDocumentV1(
      Integer schemaVersion,
      Map<String, String> bones,
      Map<String, String> clips,
      Look look,
      Sampling sampling,
      List<String> ignore) {
    this(
        schemaVersion,
        null,
        null,
        null,
        null,
        Map.of(),
        bones,
        clips,
        look,
        Map.of(),
        sampling,
        ignore,
        null);
  }

  public MappingDocumentV1 {
    bones = Map.copyOf(bones == null ? Map.of() : bones);
    clips = Map.copyOf(clips == null ? Map.of() : clips);
    rootRoles = Map.copyOf(rootRoles == null ? Map.of() : rootRoles);
    stateMappings = Map.copyOf(stateMappings == null ? Map.of() : stateMappings);
    ignore = List.copyOf(ignore == null ? List.of() : ignore);
  }

  public record Look(
      String composition,
      Double neckInfluence,
      Double headInfluence,
      Boolean allowOverrotation,
      Map<String, Double> limits) {
    public Look(
        String composition, Double neckInfluence, Double headInfluence, Boolean allowOverrotation) {
      this(composition, neckInfluence, headInfluence, allowOverrotation, Map.of());
    }
  }

  public record StateMapping(String clip, String mode, Boolean optional, Integer requestedFps) {}

  public record Sampling(Integer requestedFps) {}

  public record DiagnosticPolicy(Boolean warningsAsErrors, Boolean ignoreUnsupported) {}
}
