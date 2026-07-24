package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedAnimationParser {
  private String source = "animations";
  Result<CpmPersistedAnimationV1> parse(String entryName, byte[] bytes, CpmArtifactLimits limits) {
    source = entryName == null ? "animations" : entryName;
    if (entryName == null || !entryName.matches("animations/(?:[vcg]_[^/]+)\\.json")) return Result.failure(err("invalid animation entry", "/"));
    var root = new CpmBoundedJsonParser(limits).parse(bytes, entryName, DiagnosticCodes.CPM_ANIMATION_INVALID);
    if (!root.success()) return Result.failure(root.diagnostics());
    JsonNode n=root.value(); var bag=new DiagnosticBag();
    var allowed=Set.of("additive","duration","frames","interpolator","loop","name","priority");
    var fields=n.fieldNames(); while(fields.hasNext()){String f=fields.next(); if(!allowed.contains(f)) bag=bag.add(feature("unsupported animation field", "/"+f));}
    String file=entryName.substring(entryName.lastIndexOf('/')+1, entryName.length()-5), logical=n.path("name").isTextual()?n.path("name").textValue():file;
    CpmPersistedAnimationKind kind=file.startsWith("v_")?CpmPersistedAnimationKind.VANILLA:file.startsWith("c_")?CpmPersistedAnimationKind.CUSTOM:CpmPersistedAnimationKind.GESTURE;
    JsonNode d=n.get("duration"), frames=n.get("frames"), loop=n.get("loop"), interp=n.get("interpolator");
    JsonNode additive=n.get("additive");
    if(additive==null||!additive.isBoolean()) bag=bag.add(err("additive must be boolean", "/additive"));
    if(d==null||!d.isIntegralNumber()||!d.canConvertToInt()||d.intValue()<=0) bag=bag.add(err("duration must be a positive integer", "/duration"));
    if(loop==null||!loop.isBoolean()) bag=bag.add(err("loop must be boolean", "/loop"));
    if(interp==null||!interp.isTextual()) bag=bag.add(err("interpolator is required", "/interpolator"));
    if(frames==null||!frames.isArray()||frames.isEmpty()) bag=bag.add(err("frames must be non-empty array", "/frames"));
    if(bag.hasErrors()) return Result.failure(bag);
    boolean looping=loop.booleanValue(); CpmPersistedInterpolator pi;
    try { pi=CpmPersistedInterpolator.fromPersistedValue(interp.textValue()); } catch (IllegalArgumentException ex) { return Result.failure(err("invalid interpolator", "/interpolator")); }
    if(pi!=CpmPersistedInterpolator.NO_INTERPOLATE && looping != pi.persistedValue().endsWith("_loop")) return Result.failure(err("interpolator does not match loop", "/interpolator"));
    var out=new ArrayList<CpmPersistedFrameV1>();
    for(int i=0;i<frames.size();i++){JsonNode f=frames.get(i), cs=f.get("components"); if(!f.isObject()||cs==null||!cs.isArray()){bag=bag.add(err("invalid frame", "/frames/"+i));continue;} if(cs.size()>limits.maxComponentsPerFrame()){bag=bag.add(err(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"component limit exceeded", "/frames/"+i+"/components"));continue;} var comps=new ArrayList<CpmPersistedFrameComponentV1>(); var seen=new HashSet<Long>(); for(int j=0;j<cs.size();j++){JsonNode c=cs.get(j); String cp="/frames/"+i+"/components/"+j; if(!c.isObject()||!c.path("storeID").isIntegralNumber()||!c.path("pos").isObject()||!c.path("rotation").isObject()||!c.path("scale").isObject()||!c.path("color").isTextual()||!c.path("show").isBoolean()){bag=bag.add(err("invalid component", cp));continue;} try {var pos=vec(c.path("pos"));var rot=vec(c.path("rotation"));var scale=vec(c.path("scale")); if(!seen.add(c.path("storeID").longValue())) bag=bag.add(err("duplicate component storeID",cp+"/storeID")); if(!c.path("color").textValue().matches("[0-9A-Fa-f]{6}")) bag=bag.add(err("invalid color",cp+"/color")); comps.add(new CpmPersistedFrameComponentV1(c.path("storeID").longValue(),pos,rot,c.path("color").textValue(),c.path("show").booleanValue(),scale,j,cp));} catch(IllegalArgumentException ex){bag=bag.add(err("invalid vector",cp));}} out.add(new CpmPersistedFrameV1(i,comps,"/frames/"+i));}
    if(bag.hasErrors()) return Result.failure(bag);
    return Result.success(new CpmPersistedAnimationV1(entryName,logical,kind,additive.booleanValue(),d.intValue(),n.has("priority")?n.get("priority").intValue():0,looping,pi,out),bag);
  }
  private static CpmPersistedVec3 vec(JsonNode n){if(!n.isObject()||!n.get("x").isNumber()||!n.get("y").isNumber()||!n.get("z").isNumber()) throw new IllegalArgumentException("vector"); return new CpmPersistedVec3(n.get("x").doubleValue(),n.get("y").doubleValue(),n.get("z").doubleValue());}
  private Diagnostic err(String m,String p){return err(DiagnosticCodes.CPM_ANIMATION_INVALID,m,p);}
  private Diagnostic err(String code,String m,String p){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath(source),null,null,p,null),m,"repair animation",null,null,new TreeMap<>());}
  private Diagnostic feature(String m,String p){return err(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED,m,p);}
}
