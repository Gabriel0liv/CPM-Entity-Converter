#!/usr/bin/env python3
"""NON_PRODUCTION: deterministic S003 fixture generator and structural checks."""

from __future__ import annotations

import hashlib
import json
import math
import struct
import zlib
import zipfile
from io import BytesIO
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = ROOT / "artifacts"
FIXED_ZIP_TIME = (1980, 1, 1, 0, 0, 0)


def canonical_json(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True,
                       separators=(",", ":")) + "\n").encode("utf-8")


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(
        ">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def make_png(width: int = 64, height: int = 64) -> bytes:
    # Authorial checker texture; RGBA, no external asset.
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            light = ((x // 8) + (y // 8)) % 2 == 0
            row.extend((220, 180 if light else 80, 120 if light else 200, 255))
        rows.append(bytes(row))
    return (b"\x89PNG\r\n\x1a\n" +
            png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) +
            png_chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) +
            png_chunk(b"IEND", b""))


def root(part_id: str, children: list[dict] | None = None) -> dict:
    value = {
        "id": part_id,
        "pos": {"x": 0, "y": 0, "z": 0},
        "rotation": {"x": 0, "y": 0, "z": 0},
        "show": False,
    }
    if children is not None:
        value["children"] = children
    return value


def cube(store_id: int = 1000) -> dict:
    return {
        "color": "ffffff",
        "mcScale": 0,
        "mirror": False,
        "name": "Spike Cube",
        "offset": {"x": -2, "y": -4, "z": -2},
        "pos": {"x": 0, "y": 0, "z": 0},
        "rotation": {"x": 0, "y": 0, "z": 0},
        "rscale": {"x": 1, "y": 1, "z": 1},
        "scale": {"x": 1, "y": 1, "z": 1},
        "show": True,
        "size": {"x": 4, "y": 4, "z": 4},
        "storeID": store_id,
        "texture": True,
        "textureSize": 1,
        "u": 0,
        "v": 0,
    }


def config(elements: list[dict] | None = None, texture: bool = False) -> dict:
    value: dict = {"version": 1}
    if elements is not None:
        value["elements"] = elements
    if texture:
        value.update({
            "skinSize": {"x": 64, "y": 64},
            "skinType": "default",
            "textures": {"skin": {"anim": [], "customGridSize": False}},
        })
    return value


def standing_animation() -> dict:
    components = []
    for angle in (0, 5):
        components.append({"components": [{
            "color": "ffffff",
            "pos": {"x": 0, "y": 0, "z": 0},
            "rotation": {"x": 0, "y": angle, "z": 0},
            "scale": {"x": 1, "y": 1, "z": 1},
            "show": True,
            "storeID": 1000,
        }]})
    return {
        "additive": True,
        "duration": 1000,
        "frames": components,
        "interpolator": "linear_loop",
        "loop": True,
        "name": "minimal-standing",
        "priority": 0,
    }


def cases() -> dict[str, dict[str, bytes]]:
    texture = make_png()
    return {
        "M0": {"config.json": canonical_json(config())},
        "M1": {"config.json": canonical_json(config(texture=True)), "skin.png": texture},
        "M2": {"config.json": canonical_json(config([root("body")]))},
        "M3": {"config.json": canonical_json(config([root("body", [cube()])], True)), "skin.png": texture},
        "M4": {"config.json": canonical_json(config([root("body"), root("head")], True)), "skin.png": texture},
        "M5": {
            "animations/v_standing_minimal.json": canonical_json(standing_animation()),
            "config.json": canonical_json(config([root("body", [cube()])], True)),
            "skin.png": texture,
        },
    }


def make_zip(entries: dict[str, bytes]) -> bytes:
    stream = BytesIO()
    with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED,
                         compresslevel=9) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, FIXED_ZIP_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED,
                             compresslevel=9)
    return stream.getvalue()


def finite_tree(value: object) -> bool:
    if isinstance(value, float):
        return math.isfinite(value)
    if isinstance(value, dict):
        return all(finite_tree(v) for v in value.values())
    if isinstance(value, list):
        return all(finite_tree(v) for v in value)
    return True


def collect_ids(elements: list[dict]) -> list[int]:
    result: list[int] = []
    def walk(node: dict) -> None:
        if "storeID" in node:
            result.append(int(node["storeID"]))
        for child in node.get("children", []):
            walk(child)
    for element in elements:
        walk(element)
    return result


def verify(name: str, payload: bytes) -> dict:
    with zipfile.ZipFile(BytesIO(payload)) as archive:
        infos = archive.infolist()
        names = [entry.filename for entry in infos]
        assert names == sorted(names), (name, names)
        assert all(entry.date_time == FIXED_ZIP_TIME for entry in infos)
        parsed = {}
        for entry in infos:
            data = archive.read(entry)
            if entry.filename.endswith(".json"):
                parsed[entry.filename] = json.loads(data)
                assert finite_tree(parsed[entry.filename])
        cfg = parsed["config.json"]
        ids = collect_ids(cfg.get("elements", []))
        assert len(ids) == len(set(ids))
        refs = []
        for path, value in parsed.items():
            if path.startswith("animations/"):
                loop = value["loop"]
                interpolator = value["interpolator"]
                assert (loop and interpolator.endswith("_loop")) or (
                    not loop and interpolator.endswith("_single"))
                for frame in value["frames"]:
                    refs.extend(int(c["storeID"]) for c in frame["components"])
        assert set(refs).issubset(set(ids) | set(range(7)))
        return {
            "case": name,
            "entries": names,
            "sha256": hashlib.sha256(payload).hexdigest(),
            "size": len(payload),
            "storeIds": ids,
            "animationReferences": refs,
            "structuralChecks": "PASS",
        }


def main() -> None:
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    manifest = []
    for name, entries in cases().items():
        first = make_zip(entries)
        second = make_zip(entries)
        assert first == second, f"{name}: non-deterministic generation"
        path = ARTIFACTS / f"{name}.cpmproject"
        path.write_bytes(first)
        manifest.append(verify(name, first))
    (ARTIFACTS / "manifest.json").write_bytes(canonical_json({
        "marker": "NON_PRODUCTION",
        "cases": manifest,
    }))
    print(json.dumps({"status": "PASS", "cases": len(manifest)}, sort_keys=True))


if __name__ == "__main__":
    main()
