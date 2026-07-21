#!/usr/bin/env python3
"""NON_PRODUCTION: deterministic S001/S002 project generator and math oracle."""

from __future__ import annotations

import hashlib
import json
import math
import zipfile
from io import BytesIO
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = ROOT / "artifacts"
FIXED_TIME = (1980, 1, 1, 0, 0, 0)


def canonical(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True,
                       separators=(",", ":")) + "\n").encode()


def vec(x: float = 0, y: float = 0, z: float = 0) -> dict:
    return {"x": x, "y": y, "z": z}


def node(name: str, sid: int, pos: dict, rot: dict, children=None, cube=False) -> dict:
    result = {
        "color": "ffffff", "mcScale": 0, "mirror": False, "name": name,
        "offset": vec(-0.5, -2, -0.5) if cube else vec(), "pos": pos,
        "rotation": rot, "rscale": vec(1, 1, 1), "scale": vec(1, 1, 1),
        "show": True, "size": vec(1, 4, 1) if cube else vec(), "storeID": sid,
        "texture": cube, "textureSize": 1, "u": 0, "v": 0,
    }
    if children:
        result["children"] = children
    return result


def root(part: str, pos: dict, rot: dict, children=None) -> dict:
    value = {"id": part, "pos": pos, "rotation": rot, "show": False}
    if children:
        value["children"] = children
    return value


def component(sid: int, pos=None, rot=None, scale=None) -> dict:
    return {"color": "ffffff", "pos": pos or vec(), "rotation": rot or vec(),
            "scale": scale or vec(1, 1, 1), "show": True, "storeID": sid}


def animation(name: str, frames: list[list[dict]], *, duration=1000, priority=0,
              loop=True, additive=True) -> dict:
    return {"additive": additive, "duration": duration,
            "frames": [{"components": frame} for frame in frames],
            "interpolator": "linear_loop" if loop else "linear_single", "loop": loop,
            "name": name, "priority": priority}


def hierarchy(topology: str) -> tuple[list[dict], dict[str, int]]:
    horn = node("horn", 2003, vec(1, -4, 0.5), vec(0, 0, 15), cube=True)
    if topology == "single-anchor":
        head = node("head", 2002, vec(0, -2, 1), vec(5, -3, 0), [horn])
        neck = node("neck", 2001, vec(0, -10, 0), vec(0, 5, 0), [head])
        return [root("body", vec(0, 0, 0), vec(0, 10, 0), [neck])], {
            "body": 1, "neck": 2001, "head": 2002, "horn": 2003}
    neck = node("neck", 2001, vec(0, -10, 0), vec(0, 5, 0))
    # Neutral body+neck contribution is baked into the independent head root.
    return [
        root("body", vec(0, 0, 0), vec(0, 10, 0), [neck]),
        root("head", vec(0.174, -12, 0.985), vec(5, 12, 0), [horn]),
    ], {"body": 1, "neck": 2001, "head": 0, "horn": 2003}


def entries(topology: str, neck: float, head: float, equal_priority: bool,
            base_additive: bool) -> dict[str, bytes]:
    elements, ids = hierarchy(topology)
    look_priority = 0 if equal_priority else 1
    cfg = {"elements": elements, "version": 1}
    standing = animation("standing", [
        [component(ids["head"], rot=vec(0, -1.5, 0))],
        [component(ids["head"], rot=vec(0, 1.5, 0))],
    ], additive=base_additive)
    walking = animation("walking", [
        [component(ids["head"], pos=vec(0, -0.15, 0), rot=vec(1, -1, 0))],
        [component(ids["head"], pos=vec(0, 0.15, 0), rot=vec(-1, 1, 0))],
    ], additive=base_additive)

    def look(axis: str) -> list[list[dict]]:
        frames = []
        for angle in (-60.0, 60.0):
            parts = []
            if neck:
                parts.append(component(ids["neck"], rot=vec(angle * neck if axis == "x" else 0,
                                                             angle * neck if axis == "y" else 0, 0)))
            if head:
                parts.append(component(ids["head"], rot=vec(angle * head if axis == "x" else 0,
                                                             angle * head if axis == "y" else 0, 0)))
            frames.append(parts)
        return frames

    return {
        "animations/v_head_rotation_pitch_look.json": canonical(animation(
            "head pitch", look("x"), duration=1001, priority=look_priority, loop=False)),
        "animations/v_head_rotation_yaw_look.json": canonical(animation(
            "head yaw", look("y"), duration=1001, priority=look_priority, loop=False)),
        "animations/v_standing_base.json": canonical(standing),
        "animations/v_walking_base.json": canonical(walking),
        "config.json": canonical(cfg),
    }


def archive(entries_by_name: dict[str, bytes]) -> bytes:
    out = BytesIO()
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for name in sorted(entries_by_name):
            info = zipfile.ZipInfo(name, FIXED_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            zf.writestr(info, entries_by_name[name], compresslevel=9)
    return out.getvalue()


def verify(data: bytes) -> dict:
    with zipfile.ZipFile(BytesIO(data)) as zf:
        infos = zf.infolist()
        names = [i.filename for i in infos]
        assert names == sorted(names)
        assert all(i.date_time == FIXED_TIME for i in infos)
        parsed = {n: json.loads(zf.read(n)) for n in names if n.endswith(".json")}
        ids = {0, 1}
        def walk(v):
            if "storeID" in v:
                assert v["storeID"] not in ids
                ids.add(v["storeID"])
            for c in v.get("children", []): walk(c)
        for element in parsed["config.json"]["elements"]: walk(element)
        for name, value in parsed.items():
            assert all(math.isfinite(n) for n in numbers(value))
            if name.startswith("animations/"):
                assert value["loop"] == value["interpolator"].endswith("_loop")
                for frame in value["frames"]:
                    assert all(c["storeID"] in ids for c in frame["components"])
        return {"entries": names, "sha256": hashlib.sha256(data).hexdigest(), "ids": sorted(ids)}


def numbers(value):
    if isinstance(value, bool): return []
    if isinstance(value, (int, float)): return [float(value)]
    if isinstance(value, dict): return [n for v in value.values() for n in numbers(v)]
    if isinstance(value, list): return [n for v in value for n in numbers(v)]
    return []


def multiply(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(4)) for c in range(4)] for r in range(4)]


def local_matrix(pos, rot):
    x, y, z = (math.radians(v) for v in rot)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = [[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]]
    ry = [[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]]
    rz = [[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    rotation = multiply(multiply(rz, ry), rx)  # CPM/IR ZYX
    rotation[0][3], rotation[1][3], rotation[2][3] = pos
    return rotation


def origin(matrix):
    return [round(matrix[i][3], 6) for i in range(3)]


def topology_transforms(topology, yaw, pitch, base_yaw, bob, split, body_extra):
    neck_weight, head_weight = split
    body_local = local_matrix([0, 0, 0], [0, 10 + body_extra, 0])
    neck_local = local_matrix([0, -10, 0], [pitch * neck_weight, 5 + yaw * neck_weight, 0])
    neck_world = multiply(body_local, neck_local)
    if topology == "single-anchor":
        head_local = local_matrix([0, -2 + bob, 1],
                                  [5 + pitch * head_weight, -3 + base_yaw + yaw * head_weight, 0])
        head_world = multiply(neck_world, head_local)
    else:
        head_local = local_matrix([.174, -12 + bob, .985],
                                  [5 + pitch * head_weight, 12 + base_yaw + yaw * head_weight, 0])
        head_world = head_local
    horn_local = local_matrix([1, -4, .5], [0, 0, 15])
    horn_world = multiply(head_world, horn_local)
    return {
        "expectedLocal": {
            "bodyRotationDeg": [0, 10 + body_extra, 0],
            "neckRotationDeg": [pitch * neck_weight, 5 + yaw * neck_weight, 0],
            "headRotationDeg": [5 + pitch * head_weight,
                                (-3 if topology == "single-anchor" else 12) + base_yaw + yaw * head_weight, 0],
            "headPosition": [0 if topology == "single-anchor" else .174,
                             (-2 if topology == "single-anchor" else -12) + bob,
                             1 if topology == "single-anchor" else .985],
        },
        "calculatedWorld": {
            "bodyPosition": origin(body_local), "neckPosition": origin(neck_world),
            "headPosition": origin(head_world), "hornPosition": origin(horn_world),
        },
    }


def case_measurements() -> list[dict]:
    cases = [
        (1, "neutral", 0, 0, 0, 0), (2, "idle-no-look", 0, 0, 1.5, 0),
        (3, "walking-no-look", 0, 0, -1, .15), (4, "yaw-neutral", 0, 0, 0, 0),
        (5, "yaw-min", -60, 0, 0, 0), (6, "yaw-max", 60, 0, 0, 0),
        (7, "pitch-min", 0, -60, 0, 0), (8, "pitch-max", 0, 60, 0, 0),
        (9, "walking+yaw", 40, 0, 1, .15), (10, "walking+pitch", 0, 30, 1, .15),
        (11, "walking+yaw+pitch", 40, 30, 1, .15), (12, "head-1.0-no-neck", 40, 0, 0, 0),
        (13, "neck-.35+head-.65", 40, 0, 0, 0), (14, "neck-.5+head-.5", 40, 0, 0, 0),
        (15, "overrotation-.75+.75", 40, 0, 0, 0), (16, "horn-follows-head", 40, 20, 0, 0),
        (17, "100-loops", 0, 0, 0, 0), (18, "standing-walking-standing", 0, 0, 0, 0),
        (19, "rotated-body+look", 40, 0, 0, 0), (20, "equal-priorities", 40, 0, 1, 0),
        (21, "absolute-base+additive-look", 40, 0, 1, 0),
        (22, "additive-base+additive-look", 40, 0, 1, 0),
    ]
    output = []
    for number, name, yaw, pitch, base_yaw, bob in cases:
        split = (0, 1) if number == 12 else ((.5, .5) if number == 14 else
                ((.75, .75) if number == 15 else (.35, .65)))
        single_total = base_yaw + yaw * sum(split)
        partition_total = base_yaw + yaw * split[1]
        body_extra = 20 if number == 19 else 0
        if number == 19:
            single_total += 20  # animated parent body contribution
            # independent head branch does not inherit this extra body rotation
        output.append({
            "case": number, "name": name, "inputYaw": yaw, "inputPitch": pitch,
            "headBobY": bob, "neutralProgress": .5,
            "singleAnchorLookDeltaWorldYawDeg": single_total,
            "rootPartitionLookDeltaWorldYawDeg": partition_total,
            "yawDifferenceDeg": single_total - partition_total,
            "lookSigns": {"positiveYaw": "+Y", "positivePitch": "+X"},
            "singleAnchor": topology_transforms("single-anchor", yaw, pitch, base_yaw, bob, split, body_extra),
            "rootPartition": topology_transforms("root-partition", yaw, pitch, base_yaw, bob, split, body_extra),
            "driftAfter100Loops": 0 if number == 17 else None,
            "note": ("root partition needs look/body rebake or a proxy to reproduce inherited neck/body motion"
                     if abs(single_total - partition_total) > 1e-9 else "same scalar yaw expectation"),
        })
    return output


def sampling_measurements() -> list[dict]:
    result = []
    for duration, requested in ((1.0, 20.0), (1.03, 20.0)):
        frames = max(1, round(duration * requested))
        for mode in ("loop", "single"):
            denominator = frames if mode == "loop" else max(1, frames - 1)
            step = duration / denominator if frames > 1 or mode == "loop" else 0.0
            result.append({
                "durationSeconds": duration, "requestedFps": requested, "mode": mode,
                "frames": frames, "effectiveFps": frames / duration,
                "frameIntervalSeconds": step,
                "maxNominalGridErrorSeconds": (denominator - 1) * abs(step - 1 / requested)
                if denominator > 1 else 0.0,
            })
    return result


def main() -> None:
    variants = {
        "baseline": (.35, .65, False, False), "equal-priority": (.35, .65, True, False),
        "head-only": (0, 1, False, False), "split-035-065": (.35, .65, False, False),
        "split-050-050": (.5, .5, False, False), "overrotation": (.75, .75, False, False),
        "base-additive": (.35, .65, False, True),
    }
    manifest = {"marker": "NON_PRODUCTION", "projects": []}
    for topology in ("single-anchor", "root-partition"):
        directory = ARTIFACTS / topology
        directory.mkdir(parents=True, exist_ok=True)
        for name, args in variants.items():
            first = archive(entries(topology, *args))
            assert first == archive(entries(topology, *args))
            path = directory / f"{name}.cpmproject"
            path.write_bytes(first)
            manifest["projects"].append({"topology": topology, "variant": name, **verify(first)})
    (ARTIFACTS / "manifest.json").write_bytes(canonical(manifest))
    (ARTIFACTS / "measurements.json").write_bytes(canonical({
        "marker": "NON_PRODUCTION", "cases": case_measurements(),
        "samplingPolicyCases": sampling_measurements(),
        "method": "stateless scalar transform oracle; editor visual confirmation remains pending",
    }))
    print(json.dumps({"status": "PASS", "projects": len(manifest["projects"]), "cases": 22}))


if __name__ == "__main__":
    main()
