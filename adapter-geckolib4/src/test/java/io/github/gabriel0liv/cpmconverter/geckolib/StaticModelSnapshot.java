package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;

/** Test-only canonical serializer for the static ModelIR boundary. */
final class StaticModelSnapshot {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private StaticModelSnapshot() {}

  static JsonNode write(ModelIR model) {
    ObjectNode root = MAPPER.createObjectNode();
    ObjectNode source = root.putObject("source");
    source.put("path", model.source().path());
    source.put("format", model.source().format());
    root.put("geometryId", model.geometryId().value());
    root.set("roots", ids(model.roots()));
    ArrayNode textures = root.putArray("textures");
    for (TextureIR texture : model.textures()) {
      ObjectNode t = textures.addObject();
      t.put("path", texture.path());
      t.put("width", texture.width());
      t.put("height", texture.height());
    }
    root.putArray("clips");
    ArrayNode bones = root.putArray("bones");
    for (BoneIR bone : model.bones()) {
      ObjectNode b = bones.addObject();
      b.put("id", bone.id().value());
      b.put("name", bone.name());
      if (bone.parent() == null) b.putNull("parent");
      else b.put("parent", bone.parent().value());
      b.set("children", ids(bone.children()));
      ObjectNode bind = b.putObject("bind");
      bind.set("translation", vec(bone.bind().translation()));
      var q = bone.bind().rotation();
      ObjectNode qr = bind.putObject("rotation");
      qr.put("w", q.w());
      qr.put("x", q.x());
      qr.put("y", q.y());
      qr.put("z", q.z());
      bind.set("scale", vec(bone.bind().scale()));
      ObjectNode p = b.putObject("provenance");
      p.put("source", bone.provenance().source().value());
      p.put("pointer", bone.provenance().jsonPointer());
      ArrayNode cubes = b.putArray("cubes");
      for (CubeIR cube : bone.cubes()) cubes.add(cube(cube));
    }
    ArrayNode features = root.putArray("unsupportedFeatures");
    for (FeatureOccurrence f : model.unsupportedFeatures()) {
      ObjectNode n = features.addObject();
      n.put("feature", f.feature());
      n.put("source", f.source().source().value());
      n.put("pointer", f.source().jsonPointer());
    }
    return root;
  }

  private static ObjectNode cube(CubeIR c) {
    ObjectNode n = MAPPER.createObjectNode();
    n.put("id", c.id().value());
    n.put("boneId", c.bone().value());
    n.set("origin", vec(c.origin()));
    n.set("size", vec(c.size()));
    n.set("pivot", vec(c.pivot()));
    var q = c.rotation();
    ObjectNode r = n.putObject("rotation");
    r.put("w", q.w());
    r.put("x", q.x());
    r.put("y", q.y());
    r.put("z", q.z());
    n.put("inflate", c.inflate());
    n.put("mirror", c.mirror());
    ObjectNode p = n.putObject("provenance");
    p.put("source", c.provenance().source().value());
    p.put("pointer", c.provenance().jsonPointer());
    ObjectNode uv = n.putObject("uv");
    if (c.uv() instanceof BoxUvIR box) {
      uv.put("type", "box");
      uv.put("u", box.u());
      uv.put("v", box.v());
    } else if (c.uv() instanceof PerFaceUvIR pf) {
      uv.put("type", "per-face");
      ObjectNode faces = uv.putObject("faces");
      for (var e : pf.faces().entrySet()) {
        ObjectNode f = faces.putObject(e.getKey().name().toLowerCase());
        f.put("u", e.getValue().u());
        f.put("v", e.getValue().v());
        f.put("width", e.getValue().width());
        f.put("height", e.getValue().height());
      }
    }
    return n;
  }

  private static ArrayNode ids(Iterable<BoneId> values) {
    ArrayNode a = MAPPER.createArrayNode();
    for (BoneId v : values) a.add(v == null ? "" : v.value());
    return a;
  }

  private static ArrayNode vec(Vec3d v) {
    ArrayNode a = MAPPER.createArrayNode();
    a.add(v.x());
    a.add(v.y());
    a.add(v.z());
    return a;
  }
}
