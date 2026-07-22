package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;

/** Test-only canonical snapshot writer for the T200 boundary. */
final class GeometryParsedSnapshot {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GeometryParsedSnapshot() {}

  static JsonNode write(ParsedGeometry geometry) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("geometryId", geometry.geometryId().value());
    root.put("textureWidth", geometry.textureWidth());
    root.put("textureHeight", geometry.textureHeight());
    root.set("roots", ids(geometry.roots()));
    ArrayNode features = root.putArray("unsupportedFeatures");
    for (var feature : geometry.unsupportedFeatures()) {
      ObjectNode item = features.addObject();
      item.put("feature", feature.feature());
      item.put("source", feature.source().source().value());
      item.put("pointer", feature.source().jsonPointer());
    }
    ArrayNode bones = root.putArray("bones");
    for (ParsedBone bone : geometry.bones()) {
      ObjectNode item = bones.addObject();
      item.put("id", bone.id().value());
      item.put("sourceName", bone.sourceName());
      if (bone.parent() == null) item.putNull("parent");
      else item.put("parent", bone.parent().value());
      item.set("children", ids(bone.children()));
      item.set("bind", transform(bone));
      item.put("inflate", bone.inflate());
      item.put("mirror", bone.mirror());
      item.put("source", bone.source().source().value());
      item.put("pointer", bone.source().jsonPointer());
      ArrayNode cubes = item.putArray("cubes");
      for (ParsedCube cube : bone.cubes()) cubes.add(cube(cube));
    }
    return root;
  }

  private static ObjectNode transform(ParsedBone bone) {
    ObjectNode bind = MAPPER.createObjectNode();
    bind.set("translation", vector(bone.bindLocal().translation()));
    var q = bone.bindLocal().rotation();
    ObjectNode rotation = bind.putObject("rotation");
    rotation.put("w", q.w());
    rotation.put("x", q.x());
    rotation.put("y", q.y());
    rotation.put("z", q.z());
    bind.set("scale", vector(bone.bindLocal().scale()));
    return bind;
  }

  private static ObjectNode cube(ParsedCube cube) {
    ObjectNode item = MAPPER.createObjectNode();
    item.put("id", cube.id().value());
    item.put("boneId", cube.boneId().value());
    item.set("origin", vector(cube.origin()));
    item.set("size", vector(cube.size()));
    item.set("pivot", vector(cube.pivot()));
    item.set("rotationDegrees", vector(cube.rotationDegrees()));
    item.put("inflate", cube.inflate());
    item.put("mirror", cube.mirror());
    if (cube.rawUv() == null) item.putNull("rawUv");
    else item.put("rawUv", cube.rawUv().canonicalJson());
    item.put("source", cube.source().source().value());
    item.put("pointer", cube.source().jsonPointer());
    return item;
  }

  private static ArrayNode ids(Iterable<BoneId> values) {
    ArrayNode result = MAPPER.createArrayNode();
    for (BoneId value : values) result.add(value == null ? "" : value.value());
    return result;
  }

  private static ArrayNode vector(io.github.gabriel0liv.cpmconverter.math.Vec3d value) {
    ArrayNode result = MAPPER.createArrayNode();
    result.add(value.x());
    result.add(value.y());
    result.add(value.z());
    return result;
  }
}
