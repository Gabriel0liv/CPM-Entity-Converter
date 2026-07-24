package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedAnimationParser {
  Result<CpmPersistedAnimationV1> parse(String entryName, byte[] bytes, CpmArtifactLimits limits) {
    if (entryName == null || !entryName.matches("animations/(?:[vcg]_[^/]+)\\.json")) return Result.failure(err("invalid animation entry", "/"));
    var root = new CpmBoundedJsonParser(limits).parse(bytes, entryName, DiagnosticCodes.CPM_ANIMATION_INVALID);
    if (!root.success()) return Result.failure(root.diagnostics());
    JsonNode n=root.value(); var bag=new DiagnosticBag();
    String file=entryName.substring(entryName.lastIndexOf('/')+1, entryName.length()-5), logical=n.path("name").isTextual()?n.path("name").textValue():file;
    CpmPersistedAnimationKind kind=file.startsWith("v_")?CpmPersistedAnimationKind.VANILLA:file.startsWith("c_")?CpmPersistedAnimationKind.CUSTOM:CpmPersistedAnimationKind.GESTURE;
    JsonNode d=n.get("duration"), frames=n.get("frames"), loop=n.get("loop"), interp=n.get("interpolator");
    if(d==null||!d.isIntegralNumber()||d.intValue()<=0) bag=bag.add(err("duration must be a positive integer", "/duration"));
    if(loop==null||!loop.isBoolean()) bag=bag.add(err("loop must be boolean", "/loop"));
    if(interp==null||!interp.isTextual()) bag=bag.add(err("interpolator is required", "/interpolator"));
    if(frames==null||!frames.isArray()||frames.isEmpty()) bag=bag.add(err("frames must be non-empty array", "/frames"));
    if(bag.hasErrors()) return Result.failure(bag);
    boolean looping=loop.booleanValue(); CpmPersistedInterpolator pi;
    try { pi=CpmPersistedInterpolator.valueOf(interp.textValue().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { return Result.failure(err("invalid interpolator", "/interpolator")); }
    if(looping != (pi==CpmPersistedInterpolator.NO_INTERPOLATE || pi.name().endsWith("_LOOP"))) return Result.failure(err("interpolator does not match loop", "/interpolator"));
    var out=new ArrayList<CpmPersistedFrameV1>();
    for(int i=0;i<frames.size();i++){JsonNode f=frames.get(i), cs=f.get("components"); if(!f.isObject()||cs==null||!cs.isArray()){bag=bag.add(err("invalid frame", "/frames/"+i));continue;} var comps=new ArrayList<CpmPersistedFrameComponentV1>(); for(int j=0;j<cs.size();j++){JsonNode c=cs.get(j); if(!c.isObject()||!c.path("storeID").isIntegralNumber()||!c.path("pos").isObject()||!c.path("rotation").isObject()||!c.path("scale").isObject()||!c.path("color").isTextual()||!c.path("show").isBoolean()){bag=bag.add(err("invalid component", "/frames/"+i+"/components/"+j));continue;} comps.add(new CpmPersistedFrameComponentV1(c.path("storeID").longValue(),vec(c.path("pos")),vec(c.path("rotation")),c.path("color").textValue(),c.path("show").booleanValue(),vec(c.path("scale")),j,"/frames/"+i+"/components/"+j));} out.add(new CpmPersistedFrameV1(i,comps,"/frames/"+i));}
    if(bag.hasErrors()) return Result.failure(bag);
    return Result.success(new CpmPersistedAnimationV1(entryName,logical,kind,n.path("additive").asBoolean(false),d.intValue(),n.path("priority").asInt(0),looping,pi,out),bag);
  }
  private static CpmPersistedVec3 vec(JsonNode n){return new CpmPersistedVec3(n.path("x").asDouble(),n.path("y").asDouble(),n.path("z").asDouble());}
  private static Diagnostic err(String m,String p){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_ANIMATION_INVALID),new SourceLocation(new SourcePath("animations"),null,null,p,null),m,"repair animation",null,null,new TreeMap<>());}
}
