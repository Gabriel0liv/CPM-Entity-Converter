"""NON_PRODUCTION S004 harness; never reimplements GeckoLib semantics."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PIN = "25a41d7375bb7eeda37dadc04b1e03fe486b33e5"

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--geckolib-dir", type=Path, required=True)
    ap.add_argument("--gradle", type=Path, default=None)
    ns = ap.parse_args()
    checkout = ns.geckolib_dir
    rec = {"nonProduction": True, "commit": PIN, "fixtures": [],
           "oracle": "NOT_EXECUTED", "sourceClassMethod": "BakedAnimationsAdapter.deserialize/bakeAnimation/buildKeyframeStack; Animation/EasingType runtime"}
    try:
        got = subprocess.check_output(["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True).strip()
        rec["checkout"] = str(checkout)
        rec["checkoutCommit"] = got
    except Exception as e:
        rec["oracle"] = "BLOCKED_CHECKOUT"
        rec["error"] = str(e)
    fixtures = ROOT / "fixtures"
    gradle = ns.gradle or Path(os.environ.get("GRADLE_BIN", "gradle"))
    oracle_project = ROOT / "scripts" / "oracle"
    try:
        args = [str(gradle), "-p", str(oracle_project), "run", "--no-daemon",
                f"-PgeckoDir={checkout}", f"--args={fixtures} {PIN}"]
        completed = subprocess.run(args, text=True, capture_output=True, check=True, shell=str(gradle).lower().endswith(".bat"))
        decoder = json.JSONDecoder()
        start = completed.stdout.find("{\n  \"marker\"")
        if start < 0:
            raise RuntimeError("oracle JSON not found in Gradle output")
        observed, _ = decoder.raw_decode(completed.stdout[start:])
        rec["oracle"] = "EXECUTED"
        rec["gradleOutputTail"] = completed.stdout[-1000:]
        for item in observed.get("fixtures", []):
            source = (fixtures / f"{item['fixture']}.json").read_bytes()
            item["inputSha256"] = hashlib.sha256(source).hexdigest()
            item["sourceJson"] = json.loads(source)
            item["sourceClassMethod"] = rec["sourceClassMethod"]
            item["expected"] = expected_for(item["fixture"])
            rec["fixtures"].append(item)
    except Exception as error:
        rec["oracle"] = "BLOCKED_NO_JAVA_ORACLE"
        rec["oracleError"] = str(error)
        for p in sorted(fixtures.glob("*.json")):
            src = p.read_bytes()
            rec["fixtures"].append({"fixture": p.stem, "sourceJson": json.loads(src),
                                     "inputSha256": hashlib.sha256(src).hexdigest(),
                                     "status": "BLOCKED_NO_JAVA_ORACLE", "parserResult": None,
                                     "samples": [], "expected": expected_for(p.stem),
                                     "sourceClassMethod": rec["sourceClassMethod"]})
    (ROOT / "artifacts").mkdir(exist_ok=True)
    (ROOT / "artifacts" / "results.json").write_text(json.dumps(rec, indent=2, sort_keys=True)+"\n", encoding="utf-8")
    print(f"S004 NON_PRODUCTION oracle harness: {rec['oracle']}; no fallback evaluator used")
    return 0

def expected_for(name: str) -> dict:
    if name == "PREPOST-004": return {"parser": "pre wins; post discarded", "diagnostic": "ANIM_PRE_POST_COLLAPSED_449"}
    if name == "PREPOST-005": return {"parser": "error or rejected keyframe", "diagnostic": "ANIM_INVALID_KEYFRAME"}
    if name == "LERP-001": return {"parser": "lerp_mode ignored", "diagnostic": "ANIM_LERP_MODE_IGNORED_449"}
    if name == "PLAYBACK-005": return {"parser": "hold_on_last_frame loop type; runtime pause behavior must be observed"}
    if name == "MOLANG-003": return {"parser": "variable context required", "diagnostic": "ANIM_DYNAMIC_MOLANG_UNSUPPORTED"}
    return {"parser": "observe actual GeckoLib 4.4.9 output"}
if __name__ == "__main__":
    raise SystemExit(main())
