// NON_PRODUCTION: executes pinned GeckoLib parser, evaluator, and controller APIs.
package spike;
import com.google.gson.*;
import java.io.*; import java.nio.file.*; import java.util.*;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.keyframe.*;
import software.bernie.geckolib.core.molang.*;
import software.bernie.geckolib.core.molang.expressions.MolangValue;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import com.eliotlash.mclib.math.IValue;

public final class S004Oracle {
  private static Gson gson() { return new GsonBuilder().registerTypeAdapter(BakedAnimations.class,new BakedAnimationsAdapter()).registerTypeAdapter(Animation.Keyframes.class,new software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter()).create(); }
  private static String easingName(EasingType type) { String[] preferred={"linear","step","easeinsine","easeoutsine","catmullrom","bounce","back","elastic"}; for(String n:preferred){EasingType t=EasingType.EASING_TYPES.get(n); if(t==type)return n;} for (Map.Entry<String,EasingType> e:EasingType.EASING_TYPES.entrySet()) if(e.getValue()==type) return e.getKey(); return "unknown"; }
  private static String loopName(Animation.LoopType type) { if(type==Animation.LoopType.LOOP)return "loop"; if(type==Animation.LoopType.PLAY_ONCE)return "play_once"; if(type==Animation.LoopType.HOLD_ON_LAST_FRAME)return "hold_on_last_frame"; return "custom"; }
  private static Object value(IValue v) { if(v==null)return null; Map<String,Object> m=new LinkedHashMap<>(); m.put("value",v.get()); m.put("class",v.getClass().getName()); if(v instanceof MolangValue mv){m.put("isConstant",mv.isConstant());m.put("holder",mv.getValueHolder().getClass().getName());} return m; }
  private static List<Map<String,Object>> axis(List<Keyframe<IValue>> frames) { List<Map<String,Object>> out=new ArrayList<>(); for(Keyframe<IValue> f:frames){ Map<String,Object> m=new LinkedHashMap<>(); m.put("lengthTicks",f.length()); m.put("start",value(f.startValue())); m.put("end",value(f.endValue())); m.put("easingName",easingName(f.easingType())); List<Double> args=new ArrayList<>(); for(IValue a:f.easingArgs())args.add(a.get()); m.put("easingArgs",args); List<Double> samples=new ArrayList<>(); for(double t:new double[]{0,.25,.5,.75,1}) { double u=t*f.length(); samples.add(EasingType.lerpWithOverride(new AnimationPoint(f,u,f.length(),f.startValue().get(),f.endValue().get()),f.easingType())); } m.put("samples",samples); out.add(m);} return out; }
  private static Map<String,Object> keyframes(KeyframeStack<Keyframe<IValue>> s) { Map<String,Object> out=new LinkedHashMap<>(); out.put("x",axis(s.xKeyframes())); out.put("y",axis(s.yKeyframes())); out.put("z",axis(s.zKeyframes())); return out; }
  private static Map<String,Object> controller(Animation a) {
    GeoAnimatable stub=new GeoAnimatable(){ public void registerControllers(AnimatableManager.ControllerRegistrar c){} public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return null;} public double getTick(Object o){return 0;} };
    AnimationController<GeoAnimatable> c=new AnimationController<>(stub,s->PlayState.CONTINUE);
    Map<String,Object> m=new LinkedHashMap<>(); m.put("api","AnimationController + Animation.LoopType"); m.put("stateBefore",c.getAnimationState().name());
    boolean again=a.loopType().shouldPlayAgain(stub,c,a); m.put("shouldPlayAgain",again); m.put("stateAfterLoopDecision",c.getAnimationState().name());
    m.put("hasAnimationFinishedBeforeSet",c.hasAnimationFinished());
    // Terminal process requires a fully baked CoreGeoModel/AnimationProcessor; this
    // invocation still exercises the real loop policy and controller state mutation.
    m.put("requestedTimes", List.of("D-epsilon","D","D+epsilon","2D"));
    m.put("terminalTimeline", "BLOCKED_REQUIRES_CORE_GEOMODEL_PROCESS");
    return m;
  }
  private static void collectMolang(JsonElement e,List<String> out){ if(e.isJsonPrimitive()&&e.getAsJsonPrimitive().isString()){String s=e.getAsString(); if(s.contains("query.")||s.startsWith("return"))out.add(s);} else if(e.isJsonArray())for(JsonElement x:e.getAsJsonArray())collectMolang(x,out); else if(e.isJsonObject())for(Map.Entry<String,JsonElement> x:e.getAsJsonObject().entrySet())collectMolang(x.getValue(),out); }
  private static Map<String,Object> molang(JsonElement root){ List<String> exprs=new ArrayList<>(); collectMolang(root,exprs); Map<String,Object> out=new LinkedHashMap<>(); List<Object> obs=new ArrayList<>(); for(String s:exprs)try{MolangValue v=MolangParser.parseJson(new JsonPrimitive(s)); Map<String,Object> x=new LinkedHashMap<>(); x.put("source",s);x.put("parse","SUCCESS");x.put("class",v.getClass().getName());x.put("isConstant",v.isConstant());x.put("valueWithoutContext",v.get()); if(s.contains("query.anim_time")){MolangParser.INSTANCE.setMemoizedValue(MolangQueries.ANIM_TIME,()->7.0);x.put("valueWithRegisteredContext",v.get());x.put("variable","query.anim_time");} obs.add(x);}catch(Throwable t){obs.add(Map.of("source",s,"parse","FAIL","error",t.toString()));} out.put("expressions",obs); return out; }
  private static Map<String,Object> parse(Path p) throws Exception { JsonElement source=JsonParser.parseString(Files.readString(p)); JsonObject animObj=source.getAsJsonObject().getAsJsonObject("animations"); BakedAnimations baked=gson().fromJson(animObj,BakedAnimations.class); if(baked.animations().isEmpty())throw new JsonParseException("no animation"); Animation a=baked.animations().values().iterator().next(); Map<String,Object> parser=new LinkedHashMap<>(); parser.put("lengthTicks",a.length()); parser.put("loopType",loopName(a.loopType())); List<Object> bones=new ArrayList<>(); for(BoneAnimation b:a.boneAnimations()){Map<String,Object> bm=new LinkedHashMap<>();bm.put("bone",b.boneName());bm.put("rotation",keyframes(b.rotationKeyFrames()));bm.put("position",keyframes(b.positionKeyFrames()));bm.put("scale",keyframes(b.scaleKeyFrames()));bones.add(bm);} parser.put("bones",bones); Map<String,Object> out=new LinkedHashMap<>(); out.put("parserObservation",parser); out.put("keyframeEvaluation",Map.of("evaluator","EasingType.lerpWithOverride","samplesLocated","parserObservation.bones.*.*.*.samples")); out.put("controllerObservation",controller(a)); out.put("policyDecision",molang(source)); return out; }
  public static void main(String[] args) throws Exception { Path dir=Paths.get(args[0]); List<Map<String,Object>> all=new ArrayList<>(); try(var stream=Files.list(dir)){for(Path p:stream.filter(x->x.toString().endsWith(".json")).sorted().toList()){Map<String,Object> r=new LinkedHashMap<>();r.put("fixture",p.getFileName().toString().replace(".json",""));r.put("sourceJson",JsonParser.parseString(Files.readString(p)));try{r.put("parserObservation",parse(p));r.put("status","PARSED");}catch(Throwable t){r.put("status","REJECTED");r.put("errorType",t.getClass().getName());r.put("message",String.valueOf(t.getMessage()));}all.add(r);}} Map<String,Object> result=new LinkedHashMap<>();result.put("marker","NON_PRODUCTION");result.put("oracleCommit",args.length>1?args[1]:"unknown");result.put("fixtures",all);System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
}
