"""NON_PRODUCTION / FIXTURE_ONLY authorial inventory audit.

This script deliberately does not claim to parse GeckoLib into ModelIR.  The
production geometry and animation parsers are T200/T202 work.  Phase 1 uses it
only to produce a deterministic inventory of authorial inputs and provenance;
the executable mapping contract is covered by FixtureSemanticContractTest.
"""

import argparse
import hashlib
import json
from pathlib import Path

FIXTURES = [
    "fixture-a-humanoid",
    "fixture-b-neck",
    "fixture-c-deep-hierarchy",
    "fixture-d-quadruped",
]
PROVENANCE_MARKERS = (
    "Author",
    "Creation date",
    "License",
    "Creation method",
    "Geometry origin",
    "Animation origin",
    "Texture origin",
    "Expected-contract origin",
    "No Mojang assets",
    "No mod assets",
    "No third-party assets",
)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def inventory(directory: Path) -> dict:
    geometry = read(directory / "geometry.geo.json")
    animations = read(directory / "animations.animation.json")
    model = geometry["minecraft:geometry"][0]
    description = model["description"]
    bones = model.get("bones", [])
    clips = animations.get("animations", {})
    return {
        "fixture": directory.name,
        "inputHashes": {
            "geometry": sha(directory / "geometry.geo.json"),
            "animations": sha(directory / "animations.animation.json"),
            "texture": sha(directory / "texture.png"),
        },
        "geometryFormat": geometry.get("format_version"),
        "animationFormat": animations.get("format_version"),
        "texture": {
            "width": description.get("texture_width"),
            "height": description.get("texture_height"),
        },
        "bones": [bone.get("name") for bone in bones],
        "roots": [bone.get("name") for bone in bones if "parent" not in bone],
        "cubes": sum(len(bone.get("cubes", [])) for bone in bones),
        "clips": list(clips),
        "provenanceComplete": provenance_complete(directory / "PROVENANCE.md"),
    }


def provenance_complete(path: Path) -> bool:
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8")
    return all(marker in text for marker in PROVENANCE_MARKERS)


def run(root: Path) -> dict:
    fixtures = [inventory(root / name) for name in FIXTURES]
    if not all(item["provenanceComplete"] for item in fixtures):
        raise SystemExit("fixture provenance is incomplete")
    return {"nonProduction": True, "fixtureOnly": True, "scope": "ORIGINAL_FIXTURE_CONTRACT", "fixtures": fixtures}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).parents[1]
    artifact = root / "artifacts" / "fixture-inventory.json"
    encoded = json.dumps(run(root), indent=2, sort_keys=True) + "\n"
    if args.check:
        if not artifact.exists() or artifact.read_text(encoding="utf-8") != encoded:
            raise SystemExit("fixture inventory artifact stale or missing")
    else:
        artifact.write_text(encoded, encoding="utf-8")
    print(encoded)


if __name__ == "__main__":
    main()
