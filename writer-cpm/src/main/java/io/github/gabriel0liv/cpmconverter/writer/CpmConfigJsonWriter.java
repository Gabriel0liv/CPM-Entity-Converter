package io.github.gabriel0liv.cpmconverter.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.projection.*;
import java.nio.charset.StandardCharsets;

final class CpmConfigJsonWriter {
  private final ObjectMapper mapper = new ObjectMapper();

  byte[] write(CpmIdentifiedProjectionV1 identified) throws com.fasterxml.jackson.core.JsonProcessingException {
    var p = identified.logicalProjection().project();
    var root = mapper.createObjectNode();
    root.put("version", p.version());
    root.put("skinType", p.skinType());
    var size = root.putObject("skinSize"); size.put("x", p.texture().width()); size.put("y", p.texture().height());
    var textures = root.putObject("textures").putObject("skin");
    textures.put("customGridSize", p.texture().customGridSize()); textures.putArray("anim");
    var elements = root.putArray("elements");
    for (var r : p.roots()) elements.add(rootJson(r, identified));
    return (mapper.writeValueAsString(root) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  private ObjectNode rootJson(CpmLogicalRootV1 r, CpmIdentifiedProjectionV1 id) {
    var n = mapper.createObjectNode();
    n.put("id", r.root().id()); n.put("show", r.show()); n.put("showInEditor", r.showInEditor()); n.put("locked", r.locked());
    n.set("pos", vec(r.transform().position())); n.set("rotation", vec(r.transform().rotationDegrees())); n.put("dup", false);
    n.put("disableVanillaAnim", r.disableVanillaAnim()); n.put("name", ""); n.put("nameColor", 0);
    var children = n.putArray("children"); for (var e : r.children()) children.add(elementJson(e, id));
    return n;
  }

  private ObjectNode elementJson(CpmLogicalElementV1 e, CpmIdentifiedProjectionV1 identified) {
    var n = mapper.createObjectNode(); var cube = e.cube();
    n.put("name", e.name()); n.put("show", cube == null || cube.show()); n.put("texture", cube == null ? false : cube.texture());
    n.put("textureSize", cube == null ? 1 : cube.textureSize());
    n.set("offset", vec(cube == null ? new Vec3d(0,0,0) : cube.offset())); n.set("pos", vec(e.transform().position())); n.set("rotation", vec(e.transform().rotationDegrees()));
    n.set("size", vec(cube == null ? new Vec3d(0,0,0) : cube.size()));
    var rs = cube == null ? e.transform().scale() : multiply(e.transform().scale(), cube.renderScale()); n.set("rscale", vec(rs));
    n.set("scale", vec(cube == null ? new Vec3d(1,1,1) : cube.meshScale()));
    n.put("u", cube != null && cube.uv() instanceof CpmBoxUvV1 b ? b.u() : 0); n.put("v", cube != null && cube.uv() instanceof CpmBoxUvV1 b ? b.v() : 0);
    if (cube != null && cube.uv() instanceof CpmPerFaceUvV1 pf) n.set("faceUV", faces(pf));
    n.put("color", cube == null ? "ffffff" : cube.color()); n.put("mirror", cube != null && cube.mirror()); n.put("mcScale", cube == null ? 0 : cube.mcScale());
    n.put("glow", cube != null && cube.glow()); n.put("recolor", cube != null && cube.recolor()); n.put("hidden", cube != null && cube.hidden());
    n.put("singleTex", false); n.put("extrude", false); n.put("locked", false); n.put("nameColor", 0);
    var sid = identified.storeIds().findByNode(e.key()).orElseThrow(); n.put("storeID", sid.value());
    var children = n.putArray("children"); for (var child : e.children()) children.add(elementJson(child, identified));
    return n;
  }

  private ObjectNode faces(CpmPerFaceUvV1 pf) {
    var o = mapper.createObjectNode();
    for (var face : CpmCubeFace.values()) if (pf.faces().containsKey(face)) {
      var f = pf.faces().get(face); var x = o.putObject(face.name().toLowerCase());
      x.put("sx", f.sx()); x.put("sy", f.sy()); x.put("ex", f.ex()); x.put("ey", f.ey()); x.put("rot", f.rotation().name().substring(4)); x.put("autoUV", f.autoUv());
    }
    return o;
  }
  private ObjectNode vec(Vec3d v) { var o = mapper.createObjectNode(); o.put("x", norm(v.x())); o.put("y", norm(v.y())); o.put("z", norm(v.z())); return o; }
  private static double norm(double d) { return d == 0.0 ? 0.0 : d; }
  private static Vec3d multiply(Vec3d a, Vec3d b) { return new Vec3d(a.x()*b.x(), a.y()*b.y(), a.z()*b.z()); }
}
