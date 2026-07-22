"""NON_PRODUCTION/FIXTURE_ONLY: execute the pinned GeckoLib oracle for A-D."""
import argparse, hashlib, json, shutil, subprocess, tempfile
from pathlib import Path

PIN = "25a41d7375bb7eeda37dadc04b1e03fe486b33e5"
TREE = "c7109828f07f88f7279b3c2d60790e8c70390c33"

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def source_clip_names(source):
    return list(source.get("animations", {}).keys())

def source_bones(clip):
    return list(clip.get("bones", {}).keys())

def source_channels(clip, bone):
    return sorted(clip.get("bones", {}).get(bone, {}).keys())

def assert_observation(name, source, observed):
    """Return assertions backed by both source JSON and the real oracle output."""
    assertions = []
    expected_names = source_clip_names(source)
    observed_clips = observed.get("parserObservation", {}).get("clips", [])
    observed_by_name = {c.get("name"): c for c in observed_clips}
    assertions.append({"name": "clipNames", "expected": sorted(expected_names),
                       "observed": sorted(observed_by_name),
                       "passed": sorted(expected_names) == sorted(observed_by_name)})
    for clip_name, clip in source.get("animations", {}).items():
        obs = observed_by_name.get(clip_name)
        assertions.append({"name": f"clip:{clip_name}:present", "expected": True,
                           "observed": obs is not None, "passed": obs is not None})
        if obs is None:
            continue
        expected_bones = sorted(source_bones(clip))
        actual_bones = sorted(b.get("bone") for b in obs.get("bones", []))
        assertions.append({"name": f"clip:{clip_name}:bones", "expected": expected_bones,
                           "observed": actual_bones, "passed": expected_bones == actual_bones})
        declared_loop = clip.get("loop")
        if declared_loop is not None:
            expected_loop = "loop" if declared_loop is True else "play_once"
            actual_loop = obs.get("loopType")
            assertions.append({"name": f"clip:{clip_name}:loop", "expected": expected_loop,
                               "observed": actual_loop,
                               "passed": actual_loop == expected_loop})
        timestamps = []
        for bone_name, channels in clip.get("bones", {}).items():
            observed_bone = next((b for b in obs.get("bones", []) if b.get("bone") == bone_name), None)
            observed_channels = set()
            if observed_bone:
                observed_channels = {k for k in ("position", "rotation", "scale")
                                     if any(observed_bone.get(k, {}).get(axis) for axis in "xyz")}
            for channel_name in source_channels(clip, bone_name):
                timestamps.extend(float(t) for t in channels[channel_name].keys())
                assertions.append({"name": f"clip:{clip_name}:bone:{bone_name}:channel:{channel_name}",
                                   "expected": True, "observed": channel_name in observed_channels,
                                   "passed": channel_name in observed_channels})
        if timestamps:
            expected_length = max(timestamps) * 20.0
            actual_length = float(obs.get("lengthTicks", 0.0))
            assertions.append({"name": f"clip:{clip_name}:length", "expected": expected_length,
                               "observed": actual_length,
                               "passed": abs(actual_length - expected_length) < 1e-6})
    return assertions

def run(ns, root):
    commit = subprocess.check_output(["git", "-C", str(ns.geckolib_dir), "rev-parse", "HEAD"], text=True).strip()
    tree = subprocess.check_output(["git", "-C", str(ns.geckolib_dir), "rev-parse", "HEAD^{tree}"], text=True).strip()
    if (commit, tree) != (PIN, TREE): raise SystemExit("unexpected GeckoLib checkout")
    with tempfile.TemporaryDirectory(prefix="cpm-fixtures-") as td:
        tmp = Path(td)
        inputs = []
        for d in sorted(root.glob("fixture-*")):
            target = tmp / f"{d.name}.json"
            shutil.copyfile(d / "animations.animation.json", target)
            inputs.append((d.name, target, sha(d / "animations.animation.json")))
        oracle = root.parent / "spikes" / "geckolib-animation-semantics" / "scripts" / "oracle"
        gradle = root.parent / ("gradlew.bat" if __import__("os").name == "nt" else "gradlew")
        cmd = [str(gradle), "-p", str(oracle), "run", f"-PgeckoDir={ns.geckolib_dir}", "--args", f"{tmp} {commit}"]
        proc = subprocess.run(cmd, cwd=oracle, text=True, capture_output=True)
        if proc.returncode: raise SystemExit(proc.stdout + proc.stderr)
        marker_key = proc.stdout.rfind('"marker"')
        marker = proc.stdout.rfind('{', 0, marker_key)
        if marker < 0: raise SystemExit("oracle marker missing")
        observed, _ = json.JSONDecoder().raw_decode(proc.stdout[marker:])
        by_name = {x["fixture"]: x for x in observed.get("fixtures", [])}
        fixtures = []
        for name, _, digest in inputs:
            item = by_name.get(name + ".json", by_name.get(name, {}))
            if not item: raise SystemExit(f"oracle omitted {name}")
            item = dict(item); item["inputSha256"] = digest
            source = json.loads((root / name / "animations.animation.json").read_text(encoding="utf-8"))
            assertions = assert_observation(name, source, item)
            item["assertions"] = assertions
            item["assertionsPassed"] = sum(1 for a in assertions if a["passed"])
            item["assertionsFailed"] = len(assertions) - item["assertionsPassed"]
            item["status"] = "PASS" if item.get("status") == "PARSED" and item["assertionsFailed"] == 0 else "FAIL"
            fixtures.append(item)
        total = sum(len(f["assertions"]) for f in fixtures)
        passed = sum(f["assertionsPassed"] for f in fixtures)
        return {"nonProduction": True, "oracleCommit": commit, "oracleTree": tree,
                "checkout": ns.geckolib_dir.name, "fixtures": fixtures,
                "assertionsTotal": total, "assertionsPassed": passed,
                "assertionsFailed": total - passed,
                "status": "PASS" if total == passed else "FAIL"}

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--geckolib-dir", type=Path, required=True)
    ap.add_argument("--check", action="store_true"); ap.add_argument("--write", action="store_true")
    ns = ap.parse_args(); root = Path(__file__).parents[1]; artifact = root / "artifacts" / "fixture-oracle-a-d.json"
    result = run(ns, root); encoded = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if ns.check:
        if not artifact.exists() or artifact.read_text(encoding="utf-8") != encoded: raise SystemExit("artifact stale or missing")
    else:
        artifact.parent.mkdir(exist_ok=True); artifact.write_text(encoded, encoding="utf-8")
    print(encoded)
if __name__ == "__main__": main()
