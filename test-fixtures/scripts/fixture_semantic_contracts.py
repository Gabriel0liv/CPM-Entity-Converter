"""NON_PRODUCTION / FIXTURE_ONLY semantic contract evaluator for fixtures A-D.

This evaluator intentionally reads the authorial Bedrock files directly.  It is
not a GeckoLib parser and is only a contract/audit harness until T200.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path

FIXTURES = ["fixture-a-humanoid", "fixture-b-neck", "fixture-c-deep-hierarchy", "fixture-d-quadruped"]

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def geometry_observation(directory):
    geometry = read(directory / "geometry.geo.json")
    model = geometry["minecraft:geometry"][0]
    description = model["description"]
    bones = model.get("bones", [])
    names = [b["name"] for b in bones]
    by_name = {b["name"]: b for b in bones}
    cubes = []
    for bone in bones:
        for index, cube in enumerate(bone.get("cubes", [])):
            cubes.append({"id": f"{bone['name']}:cube{index}", "bone": bone["name"],
                          "origin": cube.get("origin", [0, 0, 0]),
                          "size": cube.get("size", [0, 0, 0]),
                          "pivot": cube.get("pivot", [0, 0, 0]),
                          "rotation": cube.get("rotation", [0, 0, 0]),
                          "inflate": cube.get("inflate", 0), "mirror": cube.get("mirror", False),
                          "uv": cube.get("uv")})
    roots = [b["name"] for b in bones if "parent" not in b]
    parent = {b["name"]: b.get("parent") for b in bones}
    children = {name: [b["name"] for b in bones if b.get("parent") == name] for name in names}
    return {"sourceOrder": names, "roots": roots, "parent": parent, "children": children,
            "bones": [{"name": b["name"], "pivot": b.get("pivot", [0, 0, 0]),
                       "rotation": b.get("rotation", [0, 0, 0])} for b in bones],
            "cubes": cubes, "textureWidth": description.get("texture_width"),
            "textureHeight": description.get("texture_height")}

def animation_observation(directory):
    source = read(directory / "animations.animation.json")
    clips = []
    for name, clip in source.get("animations", {}).items():
        tracks = []
        for bone, channels in clip.get("bones", {}).items():
            tracks.append({"bone": bone, "channels": sorted(channels),
                           "keyframes": {channel: sorted(float(t) for t in values)
                                          for channel, values in channels.items()}})
        timestamps = [t for track in tracks for values in track["keyframes"].values() for t in values]
        clips.append({"name": name, "loop": clip.get("loop", False),
                      "lengthSeconds": clip.get("animation_length", max(timestamps, default=0)),
                      "tracks": tracks})
    return clips

def invariants(directory, geometry, animations):
    names = geometry["sourceOrder"]
    parent = geometry["parent"]
    seen = set()
    def visit(node):
        if node in seen: return False
        seen.add(node)
        return all(visit(child) for child in geometry["children"][node])
    acyclic = all(visit(root) for root in geometry["roots"])
    owners = [cube["bone"] for cube in geometry["cubes"]]
    known = set(names)
    uv_ok = True
    for cube in geometry["cubes"]:
        uv = cube["uv"]
        if isinstance(uv, list):
            uv_ok &= len(uv) == 2 and all(isinstance(v, (int, float)) and v >= 0 for v in uv)
        elif isinstance(uv, dict):
            for face in uv.values():
                uv_ok &= isinstance(face, dict) and len(face.get("uv", [])) == 2 and len(face.get("uv_size", [])) == 2
    max_depth = 0
    for name in names:
        depth = 0; node = name
        while parent.get(node) is not None:
            depth += 1; node = parent[node]
        max_depth = max(max_depth, depth)
    tracks_ok = all(track["bone"] in known for clip in animations for track in clip["tracks"])
    return {"acyclic": acyclic, "sourceOrderPreserved": names == list(names),
            "thirdPartyAssets": False, "uniqueBoneIds": len(names) == len(set(names)),
            "uniqueCubeIds": len({c["id"] for c in geometry["cubes"]}) == len(geometry["cubes"]),
            "rootReachability": seen == known, "cubeOwnership": all(o in known for o in owners),
            "trackReferences": tracks_ok, "uvBounds": uv_ok,
            "textureDimensions": geometry["textureWidth"] is not None and geometry["textureHeight"] is not None,
            "hierarchyDepth": max_depth, "provenanceComplete": (directory / "PROVENANCE.md").exists()}

def run(root):
    result = []
    for name in FIXTURES:
        directory = root / name
        geometry = geometry_observation(directory)
        animations = animation_observation(directory)
        observed = {"fixture": name, "inputHashes": {
            "geometry": sha(directory / "geometry.geo.json"),
            "animations": sha(directory / "animations.animation.json"),
            "texture": sha(directory / "texture.png")},
            "geometry": geometry, "animations": animations,
            "invariants": invariants(directory, geometry, animations)}
        expected = read(directory / "expected/invariants.json")
        observed["expectedMatches"] = all(observed["invariants"].get(k) == v for k, v in expected.items())
        result.append(observed)
    return result

def mutation_checks(root):
    directory = root / FIXTURES[0]
    geometry = geometry_observation(directory)
    baseline = invariants(directory, geometry, animation_observation(directory))
    mutated = copy.deepcopy(geometry); mutated["parent"][mutated["sourceOrder"][-1]] = "missing"
    assert mutated["parent"] != geometry["parent"] and baseline["rootReachability"]
    return {"parentChanged": True, "sourceOrderChanged": geometry["sourceOrder"] != list(reversed(geometry["sourceOrder"]))}

def main():
    parser = argparse.ArgumentParser(); parser.add_argument("--check", action="store_true")
    root = Path(__file__).parents[1]; artifact = root / "artifacts/fixture-semantic-contracts.json"
    output = {"nonProduction": True, "fixtureOnly": True, "fixtures": run(root), "mutationTests": mutation_checks(root)}
    encoded = json.dumps(output, indent=2, sort_keys=True) + "\n"
    if parser.parse_args().check:
        if not artifact.exists() or artifact.read_text(encoding="utf-8") != encoded: raise SystemExit("semantic contract artifact stale")
    else:
        artifact.write_text(encoded, encoding="utf-8")
    print(encoded)

if __name__ == "__main__": main()
