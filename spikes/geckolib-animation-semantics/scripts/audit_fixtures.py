"""NON_PRODUCTION: audit fixture identity and structured semantic expectations."""
from __future__ import annotations
import hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]; FIX = ROOT / "fixtures"
names = sorted(p.stem for p in FIX.glob("*.json"))
purposes = {
 "PREPOST-001":"Only pre value", "PREPOST-002":"Only post value", "PREPOST-003":"Equal pre and post", "PREPOST-004":"Different pre and post", "PREPOST-005":"Invalid empty keyframe",
 "LERP-001":"Channel lerp_mode catmullrom", "LERP-002":"Per-keyframe catmullrom", "EASE-001":"Linear easing", "EASE-002":"Step easing", "EASE-003":"Sine easing", "EASE-004":"Unknown custom easing", "EASE-005":"Bounce easing", "EASE-006":"Back easing with arguments", "EASE-007":"Elastic easing with arguments",
 "MOLANG-001":"Numeric constants", "MOLANG-002":"Constant Molang expression", "MOLANG-003":"Runtime-dependent registered query",
 "PLAYBACK-001":"Boolean loop true", "PLAYBACK-002":"Boolean loop false", "PLAYBACK-003":"String loop", "PLAYBACK-004":"Play once", "PLAYBACK-005":"Hold on last frame", "PLAYBACK-006":"Unknown custom loop",
 "LENGTH-001":"Explicit animation length", "LENGTH-002":"Implicit length from all last keyframes", "LENGTH-003":"Implicit unbounded empty animation", "LENGTH-004":"Short explicit length",
 "KEYFRAME-001":"Textual timestamps out of order", "KEYFRAME-002":"Duplicate timestamp raw JSON", "KEYFRAME-003":"Different channel timelines", "KEYFRAME-004":"Absent channels",
 "ROTATION-001":"Rotation above 180 degrees", "ROTATION-002":"Rotation above 360 degrees", "ROTATION-003":"350 to 10 scalar transition", "POSITION-001":"Position offsets", "SCALE-001":"Normal scale", "SCALE-002":"Zero scale"}
distinct = {"MOLANG-003":["MOLANG-001","MOLANG-002"], "LENGTH-002":["LENGTH-001"], "KEYFRAME-003":["KEYFRAME-004"]}
rejection = {"PREPOST-005":"EXPECTED_REJECTION"}
runtime = {"MOLANG-003":"CONTEXT_REQUIRED", "PLAYBACK-005":"CONTROLLER_REQUIRED", "PLAYBACK-004":"CONTROLLER_REQUIRED"}

def main() -> int:
    errors=[]; entries=[]
    expected = set(purposes)
    if set(names) != expected: errors.append(f"fixture set mismatch missing={sorted(expected-set(names))} extra={sorted(set(names)-expected)}")
    for name in names:
        p=FIX/f"{name}.json"; raw=p.read_text(encoding="utf-8"); sha=hashlib.sha256(p.read_bytes()).hexdigest()
        try: obj=json.loads(raw)
        except Exception as e: errors.append(f"{name}: invalid JSON {e}"); continue
        if not isinstance(obj.get("animations"),dict): errors.append(f"{name}: missing animations")
        if name=="LENGTH-002" and "animation_length" in obj["animations"]["a"]: errors.append("LENGTH-002 still has animation_length")
        if name=="MOLANG-003" and "query.anim_time" not in raw: errors.append("MOLANG-003 lacks registered query")
        if name=="MOLANG-002" and "return" not in raw: errors.append("MOLANG-002 lacks textual expression")
        if name=="KEYFRAME-002" and len(re.findall(r'"0\.5"\s*:',raw)) != 2: errors.append("KEYFRAME-002 is not deliberate duplicate raw JSON")
        entries.append({"fixture":name,"path":f"fixtures/{p.name}","sha256":sha,"purpose":purposes[name],"expectedDistinctFrom":distinct.get(name,[]),"expectedParserStatus":"REJECTED" if name in rejection else "SUCCESS","expectedRuntimeStatus":runtime.get(name,"OBSERVED")})
    hashes={n:hashlib.sha256((FIX/f"{n}.json").read_bytes()).hexdigest() for n in names}
    for n,others in distinct.items():
        for o in others:
            if hashes.get(n)==hashes.get(o): errors.append(f"{n} and {o} unexpectedly identical")
    manifest={"marker":"NON_PRODUCTION","fixtures":entries}
    (ROOT/"fixture-manifest.json").write_text(json.dumps(manifest,indent=2,sort_keys=True)+"\n",encoding="utf-8")
    expectations={"marker":"NON_PRODUCTION","fixtures":{e["fixture"]:{"parser":{"status":e["expectedParserStatus"]},"runtime":{"status":e["expectedRuntimeStatus"]}} for e in entries}}
    (ROOT/"expected"/"expectations.json").write_text(json.dumps(expectations,indent=2,sort_keys=True)+"\n",encoding="utf-8")
    print(json.dumps({"status":"PASS" if not errors else "FAIL","fixtures":len(entries),"errors":errors},indent=2))
    return 0 if not errors else 1
if __name__ == "__main__": raise SystemExit(main())
