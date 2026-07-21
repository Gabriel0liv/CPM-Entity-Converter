"""NON_PRODUCTION: deterministic, author-written GeckoLib semantic fixtures."""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "fixtures"
OUT.mkdir(exist_ok=True)

NAMES = """PREPOST-001 PREPOST-002 PREPOST-003 PREPOST-004 PREPOST-005
LERP-001 LERP-002 EASE-001 EASE-002 EASE-003 EASE-004 EASE-005 EASE-006 EASE-007
MOLANG-001 MOLANG-002 MOLANG-003
PLAYBACK-001 PLAYBACK-002 PLAYBACK-003 PLAYBACK-004 PLAYBACK-005 PLAYBACK-006
LENGTH-001 LENGTH-002 LENGTH-003 LENGTH-004
KEYFRAME-001 KEYFRAME-002 KEYFRAME-003 KEYFRAME-004
ROTATION-001 ROTATION-002 ROTATION-003 POSITION-001 SCALE-001 SCALE-002""".split()

def base():
    return {"animations": {"a": {"animation_length": 1.0, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [1, 1, 1]}}
    }}}}

def write(name: str, obj: dict):
    (OUT / f"{name}.json").write_text(json.dumps(obj, sort_keys=True, indent=2) + "\n", encoding="utf-8")

for name in NAMES:
    obj = base(); anim = obj["animations"]["a"]; head = anim["bones"]["head"]
    if name == "PREPOST-001": head["rotation"] = {"0.5": {"pre": [10, 20, 30]}}
    elif name == "PREPOST-002": head["rotation"] = {"0.5": {"post": [10, 20, 30]}}
    elif name == "PREPOST-003": head["rotation"] = {"0.5": {"pre": [1, 2, 3], "post": [1, 2, 3]}}
    elif name == "PREPOST-004": head["rotation"] = {"0.5": {"pre": [1, 2, 3], "post": [4, 5, 6]}}
    elif name == "PREPOST-005": head["rotation"] = {"0.5": {}}
    elif name == "LERP-001": head["rotation"] = {"lerp_mode": "catmullrom", "0.0": [0, 0, 0], "1.0": [1, 1, 1]}
    elif name == "LERP-002": head["rotation"] = {"0.0": [0, 0, 0], "1.0": {"vector": [1, 1, 1], "easing": "catmullrom"}}
    elif name in {"EASE-001", "EASE-002", "EASE-003", "EASE-004", "EASE-005", "EASE-006", "EASE-007"}:
        easing = {"EASE-001": "linear", "EASE-002": "step", "EASE-003": "easeinsine",
                  "EASE-004": "custom_unknown", "EASE-005": "bounce",
                  "EASE-006": "back", "EASE-007": "elastic"}[name]
        easing = {"EASE-005": "easeinbounce", "EASE-006": "easeinback", "EASE-007": "easeinelastic"}.get(name, easing)
        end = {"vector": [1, 1, 1], "easing": easing}
        if name in {"EASE-006", "EASE-007"}: end["easingArgs"] = [1.2, 0.35]
        head["rotation"] = {"0.0": [0, 0, 0], "1.0": end}
    elif name == "MOLANG-001": head["rotation"] = {"0.0": [2, 3, 4], "1.0": [2, 3, 4]}
    elif name == "MOLANG-002": head["rotation"] = {"0.0": ["return 2 + 3", "return 4", "return 5"], "1.0": ["return 2 + 3", "return 4", "return 5"]}
    elif name == "MOLANG-003": head["rotation"] = {"0.0": ["query.anim_time", "query.anim_time", "query.anim_time"], "1.0": ["query.anim_time", "query.anim_time", "query.anim_time"]}
    elif name.startswith("PLAYBACK-"):
        anim["loop"] = {"PLAYBACK-001": True, "PLAYBACK-002": False, "PLAYBACK-003": "loop",
                         "PLAYBACK-004": "play_once", "PLAYBACK-005": "hold_on_last_frame",
                         "PLAYBACK-006": "custom"}[name]
    elif name == "LENGTH-002":
        anim.pop("animation_length")
        head["rotation"] = {"0.0": [0, 0, 0], "0.5": [1, 1, 1], "1.5": [2, 2, 2]}
        anim["bones"]["tail"] = {"position": {"0.0": [0, 0, 0], "2.0": [3, 3, 3]}}
    elif name == "LENGTH-003":
        anim.pop("animation_length"); anim["bones"] = {}
    elif name == "LENGTH-004": anim["animation_length"] = 0.5
    elif name == "KEYFRAME-001": head["rotation"] = {"1.0": [10, 0, 0], "0.0": [0, 0, 0], "0.5": [5, 0, 0]}
    elif name == "KEYFRAME-003":
        head["rotation"] = {"0.0": [0, 0, 0], "0.5": [1, 1, 1], "1.0": [2, 2, 2]}
        head["position"] = {"0.0": [0, 0, 0], "1.0": [1, 1, 1]}
        head["scale"] = {"0.25": [1, 1, 1], "0.75": [2, 2, 2]}
    elif name == "KEYFRAME-004":
        head.pop("rotation"); head["position"] = {"0.0": [1, 2, 3]}
    elif name == "ROTATION-001": head["rotation"]["1.0"] = [190, 0, 0]
    elif name == "ROTATION-002": head["rotation"]["1.0"] = [720, 0, 0]
    elif name == "ROTATION-003": head["rotation"] = {"0.0": [350, 0, 0], "1.0": [10, 0, 0]}
    elif name == "POSITION-001": head.clear(); head["position"] = {"0.0": [1, 2, 3], "1.0": [2, 3, 4]}
    elif name == "SCALE-001": head.clear(); head["scale"] = {"0.0": [1, 1, 1], "1.0": [2, 2, 2]}
    elif name == "SCALE-002": head.clear(); head["scale"] = {"0.0": [0, 0, 0], "1.0": [0, 0, 0]}
    write(name, obj)

# Gson's JsonParser accepts the last duplicate property. Keep this deliberate raw
# fixture outside the Python dict path so the audit can inspect it specially.
(OUT / "KEYFRAME-002.json").write_text(
    '{"animations":{"a":{"animation_length":1.0,"bones":{"head":{"rotation":{"0.0":[0,0,0],"0.5":[5,0,0],"0.5":[9,0,0],"1.0":[1,0,0]}}}}}}\n',
    encoding="utf-8")
print(f"generated {len(NAMES)} fixtures")
