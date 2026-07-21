"""NON_PRODUCTION: record the real GeckoLib oracle boundary for fixtures A-D.

The S004 runner is intentionally fixture-manifest based and cannot consume the
geometry fixture directory without adding a production parser. This report
therefore records the checked-out oracle identity and marks A-D blocked rather
than claiming parser execution that did not occur.
"""
import argparse, hashlib, json, subprocess
from pathlib import Path

PIN = "25a41d7375bb7eeda37dadc04b1e03fe486b33e5"
TREE = "c7109828f07f88f7279b3c2d60790e8c70390c33"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--geckolib-dir", type=Path, required=True)
    ns = ap.parse_args()
    checkout = ns.geckolib_dir.resolve()
    commit = subprocess.check_output(["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True).strip()
    tree = subprocess.check_output(["git", "-C", str(checkout), "rev-parse", "HEAD^{tree}"], text=True).strip()
    if commit != PIN or tree != TREE:
        raise SystemExit(f"unexpected GeckoLib checkout: {commit} {tree}")
    root = Path(__file__).parents[1]
    fixtures = []
    for directory in sorted(root.glob("fixture-*")):
        fixtures.append({
            "fixture": directory.name,
            "inputHashes": {
                "geometry": sha(directory / "geometry.geo.json"),
                "animations": sha(directory / "animations.animation.json"),
            },
            "parseResult": "BLOCKED",
            "reason": "S004 oracle accepts its own manifest; consuming A-D here would require a production geometry/animation parser",
        })
    result = {"nonProduction": True, "oracleCommit": commit, "oracleTree": tree, "checkout": checkout.name, "fixtures": fixtures}
    (root / "artifacts").mkdir(exist_ok=True)
    (root / "artifacts" / "fixture-oracle-a-d.json").write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2, sort_keys=True))

if __name__ == "__main__":
    main()
