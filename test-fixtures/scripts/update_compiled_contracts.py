"""NON_PRODUCTION/FIXTURE_ONLY: regenerate semantic mapping snapshots."""
import json
from pathlib import Path

root = Path(__file__).parents[1]
for directory in sorted(root.glob("fixture-*")):
    mapping = {}
    for line in (directory / "mapping.yaml").read_text(encoding="utf-8").splitlines():
        if line.startswith("bones:"):
            mapping["bones"] = line.split("{", 1)[1].rstrip("}").split(", ")
        if line.startswith("clips:"):
            mapping["clips"] = line.split("{", 1)[1].rstrip("}").split(", ")
    bones = {}
    for item in mapping["bones"]:
        key, value = item.split(": ")
        bones[key] = f"{directory.name}:{value}"
    clips = {}
    for item in mapping["clips"]:
        key, value = item.split(": ")
        clips[key] = f"{directory.name}:{value}"
    state_name = "walking" if directory.name.endswith("quadruped") else "standing"
    state_clip = clips["walk"] if state_name == "walking" else clips["idle"]
    out = {
        "boneIds": bones,
        "clipIds": clips,
        "rootRoles": {"body": bones["body"]},
        "stateMappings": {state_name: {"clipId": state_clip, "mode": "LOOP", "optional": False}},
        "sampling": {"requestedFps": 20},
        "ignoreRules": [],
    }
    if directory.name.endswith("humanoid"):
        out["look"] = {"composition": "inherited_split", "neckInfluence": 0.0, "headInfluence": 1.0, "allowOverrotation": False}
    elif directory.name.endswith("neck"):
        out["look"] = {"composition": "inherited_split", "neckInfluence": 0.35, "headInfluence": 0.65, "allowOverrotation": False}
    elif directory.name.endswith("deep-hierarchy"):
        out["look"] = {"composition": "inherited_split", "neckInfluence": 0.5, "headInfluence": 0.5, "allowOverrotation": False}
    else:
        out["look"] = {"composition": "independent", "neckInfluence": 0.0, "headInfluence": 1.0, "allowOverrotation": False}
    (directory / "expected/mapping-compiled.json").write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
