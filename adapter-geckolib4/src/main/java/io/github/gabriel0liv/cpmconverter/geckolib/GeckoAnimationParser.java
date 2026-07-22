package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/** T202 parser for Bedrock animation JSON (baseline 1.8.0). */
public final class GeckoAnimationParser {
  private static final ObjectMapper JSON = new ObjectMapper();

  public Result<List<AnimationClipIR>> parse(
      List<AnimationInput> inputs, ModelIR model, AnimationParseRequest request) {
    if (inputs == null || model == null) {
      return Result.failure(
          diag(
              null,
              DiagnosticCodes.ANIM_PARSE_ERROR,
              "inputs and model are required",
              null,
              Map.of()));
    }
    DiagnosticBag bag = new DiagnosticBag();
    Map<ClipId, AnimationClipIR> clips = new TreeMap<>(Comparator.comparing(ClipId::value));
    Map<String, BoneId> bones = new HashMap<>();
    model.bones().forEach(b -> bones.put(b.name(), b.id()));
    for (AnimationInput input : inputs) {
      JsonNode root;
      try {
        root = JSON.readTree(Files.readAllBytes(input.path()));
      } catch (IOException e) {
        bag =
            bag.add(
                diag(
                    input.logicalSource(),
                    DiagnosticCodes.INPUT_PARSE_ERROR,
                    "Unable to read animation JSON",
                    null,
                    Map.of()));
        continue;
      }
      if (root == null || !root.isObject()) {
        bag =
            bag.add(
                diag(
                    input.logicalSource(),
                    DiagnosticCodes.INPUT_PARSE_ERROR,
                    "Animation root must be an object",
                    null,
                    Map.of()));
        continue;
      }
      String version = text(root.get("format_version"));
      if (!"1.8.0".equals(version)) {
        bag =
            bag.add(
                diag(
                    input.logicalSource(),
                    DiagnosticCodes.INPUT_UNSUPPORTED_VERSION,
                    "Unsupported animation format",
                    null,
                    Map.of("formatVersion", String.valueOf(version))));
        continue;
      }
      JsonNode animations = root.get("animations");
      if (animations == null || !animations.isObject()) {
        bag =
            bag.add(
                diag(
                    input.logicalSource(),
                    DiagnosticCodes.INPUT_PARSE_ERROR,
                    "animations must be an object",
                    null,
                    Map.of()));
        continue;
      }
      Iterator<Map.Entry<String, JsonNode>> it = animations.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> e = it.next();
        ClipId id;
        try {
          id = new ClipId(e.getKey());
        } catch (IllegalArgumentException ex) {
          bag =
              bag.add(
                  diag(
                      input.logicalSource(),
                      DiagnosticCodes.INPUT_PARSE_ERROR,
                      "Clip id is empty",
                      "/animations",
                      Map.of("clipId", e.getKey())));
          continue;
        }
        if (!e.getValue().isObject()) {
          bag =
              bag.add(
                  diag(
                      input.logicalSource(),
                      DiagnosticCodes.INPUT_PARSE_ERROR,
                      "Clip must be object",
                      "/animations/" + esc(e.getKey()),
                      Map.of("clipId", id.value())));
          continue;
        }
        if (clips.containsKey(id)) {
          bag =
              bag.add(
                  diag(
                      input.logicalSource(),
                      DiagnosticCodes.IR_DUPLICATE_CLIP_ID,
                      "Duplicate clip id",
                      "/animations/" + esc(e.getKey()),
                      Map.of("clipId", id.value())));
          continue;
        }
        ParseClip parsed = parseClip(id, e.getValue(), input.logicalSource(), bones);
        bag = bag.addAll(parsed.diagnostics);
        if (parsed.clip != null) clips.put(id, parsed.clip);
      }
    }
    if (bag.hasErrors()) return Result.failure(bag);
    return Result.success(List.copyOf(clips.values()), bag);
  }

  private ParseClip parseClip(
      ClipId id, JsonNode node, SourcePath source, Map<String, BoneId> bones) {
    DiagnosticBag bag = new DiagnosticBag();
    String ptr = "/animations/" + esc(id.value());
    Playback playback = playback(node.get("loop"), source, ptr, id);
    bag = bag.addAll(playback.diagnostics);
    List<BoneTrackIR> tracks = new ArrayList<>();
    JsonNode bonesNode = node.get("bones");
    double max = 0;
    if (bonesNode != null && bonesNode.isObject()) {
      for (Map.Entry<String, JsonNode> b : iterable(bonesNode.fields())) {
        BoneId bone = bones.get(b.getKey());
        String bp = ptr + "/bones/" + esc(b.getKey());
        if (bone == null) {
          bag =
              bag.add(
                  diag(
                      source,
                      DiagnosticCodes.ANIM_BONE_NOT_FOUND,
                      "Animation bone not found",
                      bp,
                      Map.of("clipId", id.value(), "boneName", b.getKey())));
          continue;
        }
        JsonNode channels = b.getValue();
        if (!channels.isObject()) {
          bag =
              bag.add(
                  diag(
                      source,
                      DiagnosticCodes.ANIM_CHANNEL_INVALID,
                      "Bone channels must be object",
                      bp,
                      Map.of("clipId", id.value())));
          continue;
        }
        ChannelParse<Vec3d> pos =
            parseChannel(channels.get("position"), source, bp + "/position", id, b.getKey(), false);
        ChannelParse<Vec3d> scale =
            parseChannel(channels.get("scale"), source, bp + "/scale", id, b.getKey(), false);
        ChannelParse<Vec3d> rot =
            parseChannel(channels.get("rotation"), source, bp + "/rotation", id, b.getKey(), true);
        bag = bag.addAll(pos.diagnostics).addAll(scale.diagnostics).addAll(rot.diagnostics);
        max = Math.max(max, Math.max(pos.maxTime, Math.max(scale.maxTime, rot.maxTime)));
        if (pos.channel == null && scale.channel == null && rot.rotation == null) continue;
        try {
          tracks.add(
              new BoneTrackIR(
                  bone,
                  pos.channel,
                  rot.rotation,
                  scale.channel,
                  TransformMode.ADDITIVE,
                  TransformSpace.LOCAL,
                  location(source, bp)));
        } catch (IllegalArgumentException ex) {
          bag =
              bag.add(
                  diag(
                      source,
                      DiagnosticCodes.ANIM_CHANNEL_INVALID,
                      ex.getMessage(),
                      bp,
                      Map.of("clipId", id.value())));
        }
      }
    }
    tracks.sort(Comparator.comparingInt(t -> indexOf(bones, t.bone())));
    double duration = number(node.get("animation_length"));
    if (node.has("animation_length") && (!Double.isFinite(duration) || duration <= 0))
      bag =
          bag.add(
              diag(
                  source,
                  DiagnosticCodes.ANIM_ZERO_DURATION_INVALID,
                  "animation_length must be positive",
                  ptr + "/animation_length",
                  Map.of("clipId", id.value())));
    if (!node.has("animation_length")) {
      duration = max;
      if (!(duration > 0))
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_IMPLICIT_LENGTH_UNBOUNDED,
                    "Animation has no finite duration",
                    ptr,
                    Map.of("clipId", id.value())));
    }
    if (Double.isFinite(duration))
      for (BoneTrackIR t : tracks)
        for (double tm : times(t))
          if (tm > duration)
            bag =
                bag.add(
                    diag(
                        source,
                        DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION,
                        "Keyframe after duration",
                        ptr,
                        Map.of("clipId", id.value(), "timestamp", Double.toString(tm))));
    List<UnsupportedEventIR> events = new ArrayList<>();
    for (String name : List.of("sound_effects", "particle_effects", "timeline"))
      if (node.has(name)) {
        events.add(
            new UnsupportedEventIR(
                DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE,
                name,
                location(source, ptr + "/" + name)));
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE,
                    "Animation event ignored by T202",
                    ptr + "/" + name,
                    Map.of("clipId", id.value(), "event", name),
                    Severity.WARNING));
      }
    if (bag.hasErrors()) return new ParseClip(null, bag);
    try {
      return new ParseClip(
          new AnimationClipIR(
              id, duration, playback.mode, null, tracks, events, location(source, ptr)),
          bag);
    } catch (IllegalArgumentException ex) {
      return new ParseClip(
          null,
          bag.add(
              diag(
                  source,
                  DiagnosticCodes.ANIM_PARSE_ERROR,
                  ex.getMessage(),
                  ptr,
                  Map.of("clipId", id.value()))));
    }
  }

  private ChannelParse<Vec3d> parseChannel(
      JsonNode node, SourcePath source, String ptr, ClipId clip, String bone, boolean rotation) {
    if (node == null || node.isNull())
      return new ChannelParse<>(null, null, 0, new DiagnosticBag());
    DiagnosticBag bag = new DiagnosticBag();
    List<KeyframeIR<Vec3d>> keys = new ArrayList<>();
    List<SourceRotationKeyframeIR> rkeys = new ArrayList<>();
    double max = 0;
    Map<String, JsonNode> frames = new LinkedHashMap<>();
    if (node.isObject())
      node.fields()
          .forEachRemaining(
              e -> {
                if (!e.getKey().equals("lerp_mode")) frames.put(e.getKey(), e.getValue());
              });
    else frames.put("0", node);
    List<Map.Entry<String, JsonNode>> ordered = new ArrayList<>(frames.entrySet());
    ordered.sort(
        Comparator.comparingDouble(
            e -> {
              try {
                return Double.parseDouble(e.getKey());
              } catch (NumberFormatException ex) {
                return Double.POSITIVE_INFINITY;
              }
            }));
    Set<Double> seenTimes = new HashSet<>();
    for (Map.Entry<String, JsonNode> e : ordered) {
      double time;
      try {
        time = Double.parseDouble(e.getKey());
      } catch (NumberFormatException ex) {
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.IR_TIMESTAMP_INVALID,
                    "Invalid timestamp",
                    ptr + "/" + esc(e.getKey()),
                    Map.of("clipId", clip.value(), "boneName", bone)));
        continue;
      }
      if (!Double.isFinite(time) || time < 0) {
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.IR_TIMESTAMP_INVALID,
                    "Timestamp must be finite and non-negative",
                    ptr,
                    Map.of("clipId", clip.value(), "boneName", bone)));
        continue;
      }
      if (!seenTimes.add(time)) {
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_DUPLICATE_TIMESTAMP,
                    "Duplicate numeric timestamp",
                    ptr + "/" + esc(e.getKey()),
                    Map.of("clipId", clip.value(), "boneName", bone)));
        continue;
      }
      JsonNode val = e.getValue();
      if (val.isObject() && val.has("easing"))
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_CUSTOM_EASING_UNSUPPORTED,
                    "Easing parsing is deferred to T203",
                    ptr,
                    Map.of("clipId", clip.value(), "boneName", bone, "deferredTo", "T203")));
      if (val.isObject() && val.has("lerp_mode"))
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_LERP_MODE_IGNORED_449,
                    "lerp_mode ignored by GeckoLib 4.4.9",
                    ptr,
                    Map.of("clipId", clip.value()),
                    Severity.WARNING));
      JsonNode chosen = val;
      if (val.isObject() && (val.has("pre") || val.has("post"))) {
        JsonNode pre = val.get("pre"), post = val.get("post");
        if (pre == null) chosen = post;
        else {
          chosen = pre;
          if (post != null && !post.equals(pre))
            bag =
                bag.add(
                    diag(
                        source,
                        DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449,
                        "pre/post collapsed using pre",
                        ptr,
                        Map.of("clipId", clip.value(), "boneName", bone, "chosen", "pre"),
                        Severity.WARNING));
        }
      } else if (val.isObject() && val.has("vector")) chosen = val.get("vector");
      Vec3d v = parseVector(chosen, source, ptr + "/" + esc(e.getKey()), clip, bone);
      if (v == null) {
        bag =
            bag.add(
                diag(
                    source,
                    DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED,
                    "Only numeric vectors are supported; deferred to T203",
                    ptr,
                    Map.of(
                        "clipId",
                        clip.value(),
                        "boneName",
                        bone,
                        "channel",
                        rotation ? "rotation" : "value",
                        "deferredTo",
                        "T203")));
        continue;
      }
      if (!rotation && ptr.contains("/position")) v = new Vec3d(-v.x(), -v.y(), v.z());
      SourceLocation loc = location(source, ptr + "/" + esc(e.getKey()));
      if (rotation)
        rkeys.add(new SourceRotationKeyframeIR(time, v, v, InterpolationIR.LINEAR, loc));
      else keys.add(new KeyframeIR<>(time, v, v, InterpolationIR.LINEAR, loc));
      max = Math.max(max, time);
    }
    if (rotation)
      return new ChannelParse<>(
          null, new SourceRotationChannelIR(rkeys, RotationOrder.ZYX), max, bag);
    return new ChannelParse<>(
        new ChannelIR<>(
            "value",
            ptr.contains("/scale") ? TransformMode.ABSOLUTE : TransformMode.ADDITIVE,
            TransformSpace.LOCAL,
            keys),
        null,
        max,
        bag);
  }

  private Vec3d parseVector(JsonNode n, SourcePath s, String p, ClipId c, String b) {
    if (n == null || n.isNull() || n.isTextual()) return null;
    if (n.isNumber()) return new Vec3d(n.doubleValue(), n.doubleValue(), n.doubleValue());
    if (!n.isArray() || n.size() != 3) return null;
    if (!n.get(0).isNumber() || !n.get(1).isNumber() || !n.get(2).isNumber()) return null;
    return new Vec3d(n.get(0).doubleValue(), n.get(1).doubleValue(), n.get(2).doubleValue());
  }

  private record ChannelParse<T>(
      ChannelIR<T> channel,
      SourceRotationChannelIR rotation,
      double maxTime,
      DiagnosticBag diagnostics) {}

  private record ParseClip(AnimationClipIR clip, DiagnosticBag diagnostics) {}

  private record Playback(PlaybackMode mode, DiagnosticBag diagnostics) {}

  private Playback playback(JsonNode n, SourcePath s, String p, ClipId c) {
    if (n == null || n.isBoolean() && !n.booleanValue())
      return new Playback(PlaybackMode.PLAY_ONCE, new DiagnosticBag());
    if (n == null) return new Playback(PlaybackMode.PLAY_ONCE, new DiagnosticBag());
    if (n.isBoolean())
      return new Playback(
          n.booleanValue() ? PlaybackMode.LOOP : PlaybackMode.PLAY_ONCE, new DiagnosticBag());
    if (!n.isTextual())
      return new Playback(
          PlaybackMode.PLAY_ONCE,
          new DiagnosticBag()
              .add(
                  diag(
                      s,
                      DiagnosticCodes.INPUT_PARSE_ERROR,
                      "loop must be boolean or string",
                      p + "/loop",
                      Map.of("clipId", c.value()))));
    return switch (n.textValue()) {
      case "true", "loop" -> new Playback(PlaybackMode.LOOP, new DiagnosticBag());
      case "false", "play_once" -> new Playback(PlaybackMode.PLAY_ONCE, new DiagnosticBag());
      case "hold_on_last_frame" -> new Playback(PlaybackMode.HOLD, new DiagnosticBag());
      default ->
          new Playback(
              PlaybackMode.PLAY_ONCE,
              new DiagnosticBag()
                  .add(
                      diag(
                          s,
                          DiagnosticCodes.ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED,
                          "Unsupported loop type",
                          p + "/loop",
                          Map.of("clipId", c.value()))));
    };
  }

  private static int indexOf(Map<String, BoneId> bones, BoneId id) {
    int i = 0;
    for (BoneId v : bones.values()) {
      if (v.equals(id)) return i;
      i++;
    }
    return Integer.MAX_VALUE;
  }

  private static Iterable<Map.Entry<String, JsonNode>> iterable(
      Iterator<Map.Entry<String, JsonNode>> it) {
    return () -> it;
  }

  private static Set<Double> times(BoneTrackIR t) {
    Set<Double> s = new HashSet<>();
    if (t.position() != null) t.position().keyframes().forEach(k -> s.add(k.time()));
    if (t.rotation() != null) t.rotation().keyframes().forEach(k -> s.add(k.timeSeconds()));
    if (t.scale() != null) t.scale().keyframes().forEach(k -> s.add(k.time()));
    return s;
  }

  private static String text(JsonNode n) {
    return n == null || !n.isTextual() ? null : n.textValue();
  }

  private static double number(JsonNode n) {
    return n != null && n.isNumber() ? n.doubleValue() : Double.NaN;
  }

  private static String esc(String s) {
    return s.replace("~", "~0").replace("/", "~1");
  }

  private static SourceLocation location(SourcePath s, String p) {
    return new SourceLocation(s, null, null, p, null);
  }

  private static Diagnostic diag(
      SourcePath s, String code, String msg, String ptr, Map<String, String> ctx) {
    return diag(s, code, msg, ptr, ctx, Severity.ERROR);
  }

  private static Diagnostic diag(
      SourcePath s, String code, String msg, String ptr, Map<String, String> ctx, Severity sev) {
    return new Diagnostic(
        sev,
        DiagnosticCode.fromCatalog(code),
        s == null ? null : location(s, ptr),
        msg,
        "Review animation input",
        ctx.get("boneName"),
        ctx.get("clipId"),
        new TreeMap<>(ctx));
  }

  private static Diagnostic diag(
      SourcePath s,
      String code,
      String msg,
      String bone,
      Map<String, String> ctx,
      Severity sev,
      boolean x) {
    return diag(s, code, msg, bone, ctx, sev);
  }
}
