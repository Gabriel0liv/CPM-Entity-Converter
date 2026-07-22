package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.github.gabriel0liv.cpmconverter.ir.*;

final class AnimationIrSnapshot {
  private static final ObjectMapper M = new ObjectMapper();

  private AnimationIrSnapshot() {}

  static JsonNode write(java.util.List<AnimationClipIR> clips) {
    ObjectNode root = M.createObjectNode();
    ArrayNode out = root.putArray("clips");
    for (AnimationClipIR clip : clips) {
      ObjectNode c = out.addObject();
      c.put("id", clip.id().value());
      c.put("duration", clip.duration());
      c.put("playback", clip.playback().name());
      c.putNull("customLoop");
      c.set("source", source(clip.source()));
      ArrayNode tracks = c.putArray("tracks");
      for (BoneTrackIR t : clip.tracks()) {
        ObjectNode n = tracks.addObject();
        n.put("boneId", t.bone().value());
        n.put("mode", t.mode().name());
        n.put("space", t.space().name());
        n.set("source", source(t.source()));
        n.set("position", channel(t.position()));
        n.set("rotation", rotation(t.rotation()));
        n.set("scale", channel(t.scale()));
      }
      ArrayNode events = c.putArray("events");
      for (UnsupportedEventIR e : clip.events()) {
        ObjectNode n = events.addObject();
        n.put("code", e.code());
        n.put("message", e.message());
        n.set("source", source(e.source()));
      }
    }
    return root;
  }

  private static ObjectNode source(
      io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation s) {
    ObjectNode n = M.createObjectNode();
    n.put("path", s.source().value());
    n.put("pointer", s.jsonPointer());
    return n;
  }

  private static JsonNode channel(ChannelIR<io.github.gabriel0liv.cpmconverter.math.Vec3d> c) {
    if (c == null) return NullNode.getInstance();
    ObjectNode n = M.createObjectNode();
    n.put("component", c.component());
    n.put("mode", c.mode().name());
    n.put("space", c.space().name());
    ArrayNode a = n.putArray("keyframes");
    for (KeyframeIR<io.github.gabriel0liv.cpmconverter.math.Vec3d> k : c.keyframes()) {
      ObjectNode x = a.addObject();
      x.put("time", k.time());
      x.set("incoming", vec(k.incomingValue()));
      x.set("outgoing", vec(k.outgoingValue()));
      x.set("easingFromPrevious", easing(k.easingFromPrevious()));
      x.set("source", source(k.source()));
    }
    return n;
  }

  private static JsonNode rotation(SourceRotationChannelIR c) {
    if (c == null) return NullNode.getInstance();
    ObjectNode n = M.createObjectNode();
    n.put("rotationOrder", c.rotationOrder().name());
    ArrayNode a = n.putArray("keyframes");
    for (SourceRotationKeyframeIR k : c.keyframes()) {
      ObjectNode x = a.addObject();
      x.put("time", k.timeSeconds());
      x.set("incoming", vec(k.incomingValue()));
      x.set("outgoing", vec(k.outgoingValue()));
      x.set("easingFromPrevious", easing(k.easingFromPrevious()));
      x.set("source", source(k.source()));
    }
    return n;
  }

  private static ObjectNode easing(EasingIR e) {
    ObjectNode n = M.createObjectNode();
    n.put("kind", e.kind().name());
    ArrayNode args = n.putArray("args");
    e.args().forEach(args::add);
    return n;
  }

  private static ArrayNode vec(io.github.gabriel0liv.cpmconverter.math.Vec3d v) {
    ArrayNode a = M.createArrayNode();
    a.add(v.x());
    a.add(v.y());
    a.add(v.z());
    return a;
  }
}
