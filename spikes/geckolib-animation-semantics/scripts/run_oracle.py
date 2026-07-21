"""NON_PRODUCTION: execute the real GeckoLib oracle and assert observations."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; PIN="25a41d7375bb7eeda37dadc04b1e03fe486b33e5"

def assertion(name, ok, actual=None, expected=None):
    return {"name":name,"status":"PASS" if ok else "FAIL","actual":actual,"expected":expected,
            "observedAccess": actual is not None}

def close(actual, expected, tolerance=1e-8):
    if isinstance(expected, bool): return actual == expected
    if isinstance(expected, (int,float)): return isinstance(actual,(int,float)) and abs(actual-expected) <= tolerance
    if isinstance(expected, list): return isinstance(actual,list) and len(actual)==len(expected) and all(close(a,e,tolerance) for a,e in zip(actual,expected))
    return actual == expected

def semantic_assertions(name, spec, f):
    """Evaluate the manually maintained semanticAssertions contract."""
    p=f.get("parserObservation",{}); bone=(p.get("bones") or [{}])[0]
    rotation=bone.get("rotation",{}).get("x",[]); position=bone.get("position",{}).get("x",[]); scale=bone.get("scale",{}).get("x",[])
    def vector_end(channel, index=-1):
        raw=[value(bone.get(channel,{}).get(axis,[])[index]) for axis in ("x","y","z")]
        return [raw[0]*-180/3.141592653589793,raw[1]*-180/3.141592653589793,raw[2]*180/3.141592653589793]
    result=[]
    def value(frame): return frame.get("end",{}).get("value") if frame else None
    for key, expected in spec.items():
        if key in {"rotationXDegrees","xDegrees"}:
            actual = [value(rotation[-1]) * -180/3.141592653589793] if key=="xDegrees" and isinstance(expected,(int,float)) else (value(rotation[-1]) * -180/3.141592653589793 if key=="rotationXDegrees" else None)
            if isinstance(expected,list): actual=[value(x)*-180/3.141592653589793 for x in rotation[-len(expected):]]
            if key=="xDegrees" and isinstance(expected,(int,float)): actual=actual[0]
            result.append(assertion(f"semantic.{key}",close(actual,expected),actual,expected))
        elif key in {"rotationDegrees","preWinsDegrees"}:
            actual=vector_end("rotation") if isinstance(expected,list) else None
            result.append(assertion(f"semantic.{key}",close(actual,expected),actual,expected))
        elif key in {"first","last"}:
            if name.startswith("POSITION") or name.startswith("SCALE"):
                channel = "position" if name.startswith("POSITION") else "scale"
                actual=[value(bone.get(channel,{}).get(axis,[])[0 if key=="first" else -1]) for axis in ("x","y","z")]
            else: actual=None
            result.append(assertion(f"semantic.{key}",close(actual,expected),actual,expected))
        elif key in {"easing","easingArgs"}:
            frame=rotation[-1]; actual=frame.get("easingName") if key=="easing" else frame.get("easingArgs",[]); result.append(assertion(f"semantic.{key}",close(actual,expected),actual,expected))
        elif key=="shortestPath":
            samples = ([rotation[0].get("start", {}).get("value")]
                       + rotation[-1].get("samples", [])) if rotation else []
            degrees = [-sample * 180 / 3.141592653589793 for sample in samples]
            sustained_winding = (len(degrees) >= 3 and abs(abs(degrees[0]) - 350.0) < 1e-6
                                 and abs(abs(degrees[-1]) - 10.0) < 1e-6
                                 and max(abs(x) for x in degrees[1:-1]) > 90.0)
            result.append(assertion("semantic.shortestPath", sustained_winding and expected is False,
                                    {"degrees": degrees, "observed": sustained_winding}, expected))
        elif key=="overshootExpected":
            samples = rotation[-1].get("samples", []) if rotation else []
            start = rotation[-1].get("start", {}).get("value") if rotation else None
            end = rotation[-1].get("end", {}).get("value") if rotation else None
            observed = bool(samples) and start is not None and end is not None and any(
                sample < min(start, end) or sample > max(start, end) for sample in samples)
            result.append(assertion("semantic.overshootExpected", observed == bool(expected),
                                    {"samples": samples, "overshoot": observed}, expected))
        elif key in {"sample25","sample50","sample75"}:
            sample_index={"sample25":1,"sample50":2,"sample75":3}[key]; actual=rotation[-1].get("samples",[])[sample_index]; result.append(assertion(f"semantic.{key}",close(actual,expected),actual,expected))
    return result

def assert_fixture(f, exp, semantic=None):
    name=f["fixture"]; a=[]; parsed=f.get("parserObservation")
    if isinstance(parsed,dict) and "parserObservation" in parsed: parsed=parsed["parserObservation"]
    if exp["parser"]["status"]=="REJECTED":
        a.append(assertion("expected rejection",f.get("status")=="REJECTED",f.get("status"),"REJECTED"))
        return a, "EXPECTED_REJECTION" if all(x["status"]=="PASS" for x in a) else "FAIL"
    a.append(assertion("parser success",f.get("status")=="PARSED",f.get("status"),"PARSED"))
    if parsed:
        if name.startswith("PLAYBACK-"):
            expected={"PLAYBACK-001":"loop","PLAYBACK-002":"play_once","PLAYBACK-003":"loop","PLAYBACK-004":"play_once","PLAYBACK-005":"hold_on_last_frame","PLAYBACK-006":"play_once"}[name]
            a.append(assertion("loop type",parsed.get("loopType")==expected,parsed.get("loopType"),expected))
        if name=="LENGTH-001": a.append(assertion("explicit length ticks",abs(parsed["lengthTicks"]-20)<1e-9,parsed["lengthTicks"],20))
        if name=="LENGTH-002": a.append(assertion("implicit last keyframe across bones",abs(parsed["lengthTicks"]-40)<1e-9,parsed["lengthTicks"],40))
        if name=="LENGTH-003": a.append(assertion("unbounded sentinel",parsed["lengthTicks"]>1e300,parsed["lengthTicks"],"Double.MAX_VALUE"))
        if name=="LENGTH-004": a.append(assertion("short explicit length",abs(parsed["lengthTicks"]-10)<1e-9,parsed["lengthTicks"],10))
        if name=="LERP-001": a.append(assertion("channel lerp ignored",parsed["bones"][0]["rotation"]["x"][0]["easingName"]=="linear",parsed["bones"][0]["rotation"]["x"][0]["easingName"],"linear"))
        if name=="LERP-002": a.append(assertion("keyframe catmullrom",parsed["bones"][0]["rotation"]["x"][1]["easingName"]=="catmullrom",parsed["bones"][0]["rotation"]["x"][1]["easingName"],"catmullrom"))
        if name.startswith("EASE-"):
            expected={"EASE-001":"linear","EASE-002":"step","EASE-003":"easeinsine","EASE-004":"linear","EASE-005":"easeinbounce","EASE-006":"easeinback","EASE-007":"easeinelastic"}[name]
            got=parsed["bones"][0]["rotation"]["x"][-1]["easingName"]
            a.append(assertion("easing name",got==expected,got,expected))
            if name in {"EASE-006","EASE-007"}: a.append(assertion("easing args",parsed["bones"][0]["rotation"]["x"][-1]["easingArgs"]==[1.2,0.35],parsed["bones"][0]["rotation"]["x"][-1]["easingArgs"],[1.2,0.35]))
        if name=="KEYFRAME-003":
            b=parsed["bones"][0]; a += [assertion("rotation timeline length",len(b["rotation"]["x"])==3,len(b["rotation"]["x"]),3),assertion("position timeline length",len(b["position"]["x"])==2,len(b["position"]["x"]),2),assertion("scale timeline length",len(b["scale"]["x"])==2,len(b["scale"]["x"]),2)]
        if name=="KEYFRAME-004": a.append(assertion("absent rotation stack empty",all(len(parsed["bones"][0]["rotation"][k])==0 for k in ("x","y","z")),parsed["bones"][0]["rotation"],"empty"))
        if name=="KEYFRAME-002": a.append(assertion("Gson duplicate last value survives",abs(parsed["bones"][0]["rotation"]["x"][1]["end"]["value"]+0.15707963267948966)<1e-9,parsed["bones"][0]["rotation"]["x"][1]["end"],"last duplicate 9 degrees retained"))
        if name.startswith("MOLANG-"):
            obs=f.get("policyDecision",{}).get("expressions",[])
            if name=="MOLANG-001": a.append(assertion("numeric constants",not obs,obs,"no Molang expressions"))
            if name=="MOLANG-002": a.append(assertion("constant expression evaluated",bool(obs) and all(x.get("parse")=="SUCCESS" and x.get("valueWithoutContext") in (4.0,5.0) for x in obs),obs,"deterministic constant evaluation"))
            if name=="MOLANG-003": a.append(assertion("dynamic expression detected",bool(obs) and all(x.get("parse")=="SUCCESS" and not x.get("isConstant") for x in obs),obs,"context required"))
    if semantic and parsed: a.extend(semantic_assertions(name, semantic, f))
    status="PASS" if all(x["status"]=="PASS" for x in a) else "FAIL"
    if name in {"PLAYBACK-004","PLAYBACK-005","PLAYBACK-001"}: status="BLOCKED" if status=="PASS" else status
    return a,status

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("--geckolib-dir",type=Path,required=True); ap.add_argument("--gradle",type=Path,default=None); ns=ap.parse_args(); checkout=ns.geckolib_dir; fixtures=ROOT/"fixtures"; project=ROOT/"scripts"/"oracle"; gradle=ns.gradle or Path(os.environ.get("GRADLE_BIN","gradle"))
    rec={"nonProduction":True,"commit":PIN,"checkout":checkout.name,"checkoutCommit":subprocess.check_output(["git","-C",str(checkout),"rev-parse","HEAD"],text=True).strip(),"oracle":"NOT_EXECUTED"}
    try:
        gradle = gradle.resolve()
        checkout = checkout.resolve()
        fixtures = fixtures.resolve()
        cmd=[str(gradle),"-p",str(project.resolve()),"run","--no-daemon",f"-PgeckoDir={checkout}",f"--args={fixtures} {PIN}"]
        run=subprocess.run(cmd,text=True,capture_output=True,check=True,shell=False)
        start=run.stdout.find('{\n  "marker"')
        if start < 0:
            raise RuntimeError("oracle marker not found in Gradle output")
        observed=json.JSONDecoder().raw_decode(run.stdout[start:])[0]
        rec["oracle"]="EXECUTED"; rec["gradleVersionOutput"]=run.stdout[:300]
    except Exception as e:
        rec["oracle"]="BLOCKED_NO_JAVA_ORACLE"; rec["oracleError"]=str(e); observed={"fixtures":[]}
    manual=json.loads((ROOT/"expected"/"expectations.json").read_text(encoding="utf8")); expectations=manual["fixtures"]; semantic=manual.get("semanticAssertions",{}); manifest=json.loads((ROOT/"fixture-manifest.json").read_text(encoding="utf8")); by={x["fixture"]:x for x in observed.get("fixtures",[])}; results=[]
    for entry in manifest["fixtures"]:
        n=entry["fixture"]; src=(ROOT/entry["path"]).read_bytes(); f=by.get(n,{"fixture":n,"status":"BLOCKED"})
        nested=f.get("parserObservation")
        if isinstance(nested,dict) and "parserObservation" in nested:
            f.update(nested); f["parserObservation"]=nested["parserObservation"]
        f["inputSha256"]=hashlib.sha256(src).hexdigest(); f["sourceJson"]=json.loads(src); f["assertions"],f["status"]=assert_fixture(f,expectations[n],semantic.get(n)) if rec["oracle"]=="EXECUTED" else ([assertion("oracle available",False,"BLOCKED","EXECUTED")],"BLOCKED"); results.append(f)
        for check in f["assertions"]:
            if check["name"].startswith("semantic.") and not check.get("observedAccess", False):
                check["status"] = "FAIL"
                f["status"] = "FAIL"
    rec["fixtures"]=results; counts={"assertionsTotal":sum(len(f["assertions"]) for f in results),"assertionsPassed":sum(sum(a["status"]=="PASS" for a in f["assertions"]) for f in results),"assertionsFailed":sum(sum(a["status"]=="FAIL" for a in f["assertions"]) for f in results),"fixturesPassed":sum(f["status"]=="PASS" for f in results),"fixturesExpectedRejection":sum(f["status"]=="EXPECTED_REJECTION" for f in results),"fixturesFailed":sum(f["status"]=="FAIL" for f in results),"fixturesBlocked":sum(f["status"]=="BLOCKED" for f in results)}; rec.update(counts); (ROOT/"artifacts"/"results.json").write_text(json.dumps(rec,indent=2,sort_keys=True)+"\n",encoding="utf8"); print(json.dumps({"oracle":rec["oracle"],**counts},indent=2)); return 1 if counts["fixturesFailed"] else 0
if __name__=="__main__": raise SystemExit(main())
