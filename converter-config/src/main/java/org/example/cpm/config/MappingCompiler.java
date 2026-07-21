package org.example.cpm.config;
import java.util.*; import org.example.cpm.ir.*;
public final class MappingCompiler { public SemanticRigMap compile(MappingDocumentV1 d){var b=new LinkedHashMap<String,BoneId>();d.bones().forEach((k,v)->b.put(k,new BoneId(v)));var c=new LinkedHashMap<String,ClipId>();d.clips().forEach((k,v)->c.put(k,new ClipId(v)));return new SemanticRigMap(b,c,d.ignore());} }
