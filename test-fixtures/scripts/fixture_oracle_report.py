"""NON_PRODUCTION/FIXTURE_ONLY: execute the pinned GeckoLib oracle for A-D."""
import argparse, hashlib, json, shutil, subprocess, tempfile
from pathlib import Path

PIN = "25a41d7375bb7eeda37dadc04b1e03fe486b33e5"
TREE = "c7109828f07f88f7279b3c2d60790e8c70390c33"

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

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
            item["status"] = "PASS" if item.get("status") == "PARSED" else "FAIL"
            fixtures.append(item)
        return {"nonProduction": True, "oracleCommit": commit, "oracleTree": tree,
                "checkout": ns.geckolib_dir.name, "fixtures": fixtures}

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
