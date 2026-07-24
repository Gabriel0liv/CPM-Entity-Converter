package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedAnimationParser {
  Result<CpmPersistedAnimationV1> parse(String entryName, byte[] bytes, CpmArtifactLimits limits) {
    if (limits == null) return Result.failure(error(entryName, DiagnosticCodes.CPM_ANIMATION_INVALID, "limits are required", "/"));
    if (entryName == null || !entryName.matches("animations/(?:[vcg]_[^/]+)\\.json")) return Result.failure(error(entryName, DiagnosticCodes.CPM_ANIMATION_INVALID, "invalid animation entry", "/"));
    var root = new CpmBoundedJsonParser(limits).parse(bytes, entryName, DiagnosticCodes.CPM_ANIMATION_INVALID);
    if (!root.success()) return Result.failure(root.diagnostics());
    JsonNode n = root.value(); var bag = new DiagnosticBag();
    var allowed = Set.of("additive", "duration", "frames", "interpolator", "loop", "name", "priority");
    var fields = n.fieldNames(); while (fields.hasNext()) { String f = fields.next(); if (!allowed.contains(f)) bag = bag.add(error(entryName, DiagnosticCodes.CPM_FEATURE_UNSUPPORTED, "unsupported animation field", "/" + f)); }
    JsonNode additive=n.get("additive"), duration=n.get("duration"), loop=n.get("loop"), interpolator=n.get("interpolator"), frames=n.get("frames");
    if(additive==null||!additive.isBoolean()) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"additive must be boolean","/additive"));
    if(duration==null||!duration.isIntegralNumber()||!duration.canConvertToInt()||duration.intValue()<=0) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"duration must be a positive integer","/duration"));
    if(loop==null||!loop.isBoolean()) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"loop must be boolean","/loop"));
    if(interpolator==null||!interpolator.isTextual()) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"interpolator is required","/interpolator"));
    if(frames==null||!frames.isArray()||frames.isEmpty()) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"frames must be non-empty array","/frames"));
    if(frames!=null&&frames.isArray()&&frames.size()>limits.maxFramesPerAnimation()) bag=bag.add(error(entryName,DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"frame limit exceeded","/frames"));
    JsonNode name=n.get("name"), priority=n.get("priority");
    if(name!=null&&!name.isTextual()) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"name must be string","/name"));
    if(priority!=null&&(!priority.isIntegralNumber()||!priority.canConvertToInt())) bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"priority must be integer","/priority"));
    if(bag.hasErrors()) return Result.failure(bag);
    boolean looping=loop.booleanValue(); CpmPersistedInterpolator pi;
    try { pi=CpmPersistedInterpolator.fromPersistedValue(interpolator.textValue()); } catch (IllegalArgumentException ex) { return Result.failure(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"invalid interpolator","/interpolator")); }
    if(pi!=CpmPersistedInterpolator.NO_INTERPOLATE && looping != pi.persistedValue().endsWith("_loop")) return Result.failure(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"interpolator does not match loop","/interpolator"));
    String file=entryName.substring(entryName.lastIndexOf('/')+1,entryName.length()-5); CpmPersistedAnimationKind kind=file.startsWith("v_")?CpmPersistedAnimationKind.VANILLA:file.startsWith("c_")?CpmPersistedAnimationKind.CUSTOM:CpmPersistedAnimationKind.GESTURE;
    String logical=name==null?file:name.textValue(); var parsedFrames=new ArrayList<CpmPersistedFrameV1>();
    for(int i=0;i<frames.size();i++){JsonNode f=frames.get(i), cs=f.isObject()?f.get("components"):null; String fp="/frames/"+i; if(!f.isObject()||cs==null||!cs.isArray()){bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"invalid frame",fp));continue;} if(cs.size()>limits.maxComponentsPerFrame()){bag=bag.add(error(entryName,DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"component limit exceeded",fp+"/components"));continue;} var components=new ArrayList<CpmPersistedFrameComponentV1>(); var seen=new HashSet<Long>(); for(int j=0;j<cs.size();j++){JsonNode c=cs.get(j);String cp=fp+"/components/"+j; if(!c.isObject()||!c.path("storeID").isIntegralNumber()||!c.path("storeID").canConvertToLong()||!c.path("pos").isObject()||!c.path("rotation").isObject()||!c.path("scale").isObject()||!c.path("color").isTextual()||!c.path("show").isBoolean()){bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"invalid component",cp));continue;} try{long id=c.path("storeID").longValue();if(!seen.add(id))bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,"duplicate component storeID",cp+"/storeID"));var pos=vec(c.get("pos"),entryName,cp+"/pos");var rot=vec(c.get("rotation"),entryName,cp+"/rotation");var scale=vec(c.get("scale"),entryName,cp+"/scale");if(!c.path("color").textValue().matches("[0-9A-Fa-f]{6}"))throw new IllegalArgumentException("color");components.add(new CpmPersistedFrameComponentV1(id,pos,rot,c.path("color").textValue(),c.path("show").booleanValue(),scale,j,cp));}catch(IllegalArgumentException ex){bag=bag.add(error(entryName,DiagnosticCodes.CPM_ANIMATION_INVALID,ex.getMessage(),cp));}} parsedFrames.add(new CpmPersistedFrameV1(i,components,fp));}
    if(bag.hasErrors()) return Result.failure(bag); return Result.success(new CpmPersistedAnimationV1(entryName,logical,kind,additive.booleanValue(),duration.intValue(),priority==null?0:priority.intValue(),looping,pi,parsedFrames),bag);
  }
  private static CpmPersistedVec3 vec(JsonNode n,String source,String pointer){if(!n.isObject()||n.size()!=3||!n.has("x")||!n.has("y")||!n.has("z")||!n.get("x").isNumber()||!n.get("y").isNumber()||!n.get("z").isNumber())throw new IllegalArgumentException("invalid vector at "+pointer);return new CpmPersistedVec3(n.get("x").doubleValue(),n.get("y").doubleValue(),n.get("z").doubleValue());}
  private static Diagnostic error(String source,String code,String message,String pointer){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath(source==null?"<animation>":source),null,null,pointer,null),message,"repair animation",null,null,new TreeMap<>());}
}
