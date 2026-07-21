package org.example.cpm.config;
import com.fasterxml.jackson.annotation.*; import java.util.*;
@JsonIgnoreProperties(ignoreUnknown=false)
public record MappingDocumentV1(@JsonProperty("schemaVersion") Integer schemaVersion,@JsonProperty("bones") Map<String,String> bones,@JsonProperty("clips") Map<String,String> clips,@JsonProperty("look") Look look,@JsonProperty("sampling") Sampling sampling,@JsonProperty("ignore") List<String> ignore){ public MappingDocumentV1{bones=Map.copyOf(bones==null?Map.of():bones);clips=Map.copyOf(clips==null?Map.of():clips);ignore=List.copyOf(ignore==null?List.of():ignore);} public record Look(String composition,Double neckInfluence,Double headInfluence,Boolean allowOverrotation){} public record Sampling(Integer requestedFps){} }
