package io.github.gabriel0liv.cpmconverter.projection;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

final class CpmLogicalProjectionSnapshot {
  private static final ObjectMapper M = new ObjectMapper();

  static JsonNode write(CpmStaticProjection p) {
    ObjectNode root = M.createObjectNode();
    ObjectNode project = root.putObject("project");
    var pr = p.project();
    project.put("version", pr.version());
    project.put("skinType", pr.skinType());
    var tx = project.putObject("texture");
    tx.put("logicalPath", pr.texture().logicalPath());
    tx.put("width", pr.texture().width());
    tx.put("height", pr.texture().height());
    tx.put("skinType", pr.texture().skinType());
    tx.put("customGridSize", pr.texture().customGridSize());
    ArrayNode roots = project.putArray("roots");
    for (var r : pr.roots()) roots.add(root(r));
    ObjectNode index = root.putObject("index");
    index.set("boneTargets", targets(p.index().boneTargets()));
    index.set("cubeTargets", targets(p.index().cubeTargets()));
    index.set("helperTargets", helpers(p.index().helperTargets()));
    return root;
  }

  private static ObjectNode root(CpmLogicalRootV1 r) {
    ObjectNode n = M.createObjectNode();
    n.put("root", r.root().id());
    n.put("reservedId", r.root().reservedId());
    n.set("transform", transform(r.transform()));
    n.put("show", r.show());
    n.put("showInEditor", r.showInEditor());
    n.put("locked", r.locked());
    n.put("disableVanillaAnim", r.disableVanillaAnim());
    n.set("origin", origin(r.origin()));
    ArrayNode c = n.putArray("children");
    r.children().forEach(x -> c.add(element(x)));
    return n;
  }

  private static ObjectNode element(CpmLogicalElementV1 e) {
    ObjectNode n = M.createObjectNode();
    n.put("key", e.key().value());
    n.put("kind", e.kind().name());
    n.put("name", e.name());
    n.set("transform", transform(e.transform()));
    n.set("origin", origin(e.origin()));
    if (e.cube() == null) n.putNull("cube");
    else n.set("cube", cube(e.cube()));
    ArrayNode c = n.putArray("children");
    e.children().forEach(x -> c.add(element(x)));
    return n;
  }

  private static ObjectNode transform(CpmTransformV1 t) {
    ObjectNode n = M.createObjectNode();
    n.set("position", vec(t.position()));
    n.set("rotation", vec(t.rotationDegrees()));
    n.set("scale", vec(t.scale()));
    return n;
  }

  private static ArrayNode vec(io.github.gabriel0liv.cpmconverter.math.Vec3d v) {
    ArrayNode a = M.createArrayNode();
    a.add(v.x());
    a.add(v.y());
    a.add(v.z());
    return a;
  }

  private static ObjectNode origin(CpmNodeOrigin o) {
    ObjectNode n = M.createObjectNode();
    n.put("kind", o.kind().name());
    if (o.root() != null) n.put("root", o.root().id());
    if (o.boneId() != null) n.put("boneId", o.boneId().value());
    if (o.cubeId() != null) n.put("cubeId", o.cubeId().value());
    if (o.source() != null) {
      n.put("source", o.source().source().value());
      n.put("pointer", String.valueOf(o.source().jsonPointer()));
    }
    return n;
  }

  private static ObjectNode cube(CpmLogicalCubeV1 c) {
    ObjectNode n = M.createObjectNode();
    n.set("offset", vec(c.offset()));
    n.set("size", vec(c.size()));
    n.set("renderScale", vec(c.renderScale()));
    n.set("meshScale", vec(c.meshScale()));
    n.put("texture", c.texture());
    n.put("textureSize", c.textureSize());
    n.set("uv", uv(c.uv()));
    n.put("color", c.color());
    n.put("mirror", c.mirror());
    n.put("mcScale", c.mcScale());
    n.put("show", c.show());
    n.put("hidden", c.hidden());
    n.put("glow", c.glow());
    n.put("recolor", c.recolor());
    return n;
  }

  private static ObjectNode uv(CpmUvV1 u) {
    ObjectNode n = M.createObjectNode();
    if (u instanceof CpmBoxUvV1 b) {
      n.put("type", "box");
      n.put("u", b.u());
      n.put("v", b.v());
    } else {
      n.put("type", "per-face");
      ArrayNode fs = n.putArray("faces");
      for (var e : ((CpmPerFaceUvV1) u).faces().entrySet()) {
        var f = e.getValue();
        ObjectNode x = fs.addObject();
        x.put("face", e.getKey().name());
        x.put("sx", f.sx());
        x.put("sy", f.sy());
        x.put("ex", f.ex());
        x.put("ey", f.ey());
        x.put("rotation", f.rotation().name());
        x.put("autoUv", f.autoUv());
      }
    }
    return n;
  }

  private static ArrayNode targets(Map<?, CpmTargetRef> m) {
    ArrayNode a = M.createArrayNode();
    for (var e : m.entrySet()) {
      ObjectNode n = a.addObject();
      n.put("sourceId", e.getKey().toString());
      n.put("key", e.getValue().key().value());
      n.put("root", e.getValue().root());
    }
    return a;
  }

  private static ArrayNode helpers(Map<?, CpmNodeKey> m) {
    ArrayNode a = M.createArrayNode();
    for (var e : m.entrySet()) {
      ObjectNode n = a.addObject();
      n.put("sourceId", e.getKey().toString());
      n.put("key", e.getValue().value());
    }
    return a;
  }
}
