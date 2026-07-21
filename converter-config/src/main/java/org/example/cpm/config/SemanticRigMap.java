package org.example.cpm.config;
import java.util.*; import org.example.cpm.ir.*;
public record SemanticRigMap(Map<String,BoneId> bones,Map<String,ClipId> clips,List<String> ignore){public SemanticRigMap{bones=Map.copyOf(bones);clips=Map.copyOf(clips);ignore=List.copyOf(ignore);}}
