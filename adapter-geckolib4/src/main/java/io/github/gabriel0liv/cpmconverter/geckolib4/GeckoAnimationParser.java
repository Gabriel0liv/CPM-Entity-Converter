package io.github.gabriel0liv.cpmconverter.geckolib4;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoneTrackIR;
import io.github.gabriel0liv.cpmconverter.ir.ChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import io.github.gabriel0liv.cpmconverter.ir.KeyframeIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.RotationOrder;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationKeyframeIR;
import io.github.gabriel0liv.cpmconverter.ir.TransformMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformSpace;
import io.github.gabriel0liv.cpmconverter.ir.UnsupportedEventIR;
import io.github.gabriel0liv.cpmconverter.math.CoordinateBoundary;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Offline parser for GeckoLib 4.4.9 animation syntax supported by the MVP. */
public final class GeckoAnimationParser {
  private static final Vec3d POSITION_DEFAULT = Vec3d.ZERO;
  private static final Vec3d SCALE_DEFAULT = new Vec3d(1, 1, 1);
  private static final EasingMetadata LINEAR_EASING =
      new EasingMetadata(InterpolationIR.LINEAR, List.of());

  private final GeckoInputLimits limits;
  private final GeckoJsonReader json;

  public GeckoAnimationParser() {
    this(GeckoInputLimits.defaults());
  }

  public GeckoAnimationParser(GeckoInputLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
    this.json = new GeckoJsonReader(limits);
  }

  public Result<List<AnimationClipIR>> parse(Path path, ModelIR model) {
    try {
      if (path == null) return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animation path is null");
      if (model == null) return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "model is null");

      JsonNode root = json.read(path);
      if (root == null || !root.isObject()) {
        return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animation root must be an object");
      }
      JsonNode animations = root.get("animations");
      if (animations == null || !animations.isObject()) {
        return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animations object is missing");
      }
      validateKeyframeLimit(animations);

      Map<String, BoneId> bonesByName = new LinkedHashMap<>();
      model.bones().forEach(bone -> bonesByName.put(bone.name(), bone.id()));

      List<Diagnostic> diagnostics = new ArrayList<>();
      List<AnimationClipIR> clips = new ArrayList<>();
      Iterator<Map.Entry<String, JsonNode>> fields = animations.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        clips.add(parseClip(path, field.getKey(), field.getValue(), bonesByName, diagnostics));
      }
      return Result.success(List.copyOf(clips), new DiagnosticBag(diagnostics));
    } catch (GeckoJsonReader.InputLimitException exception) {
      return failure(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, exception.getMessage());
    } catch (AnimationParseException exception) {
      return failure(exception.code(), exception.getMessage());
    } catch (Exception exception) {
      return failure(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          exception.getMessage() == null ? "cannot parse animations" : exception.getMessage());
    }
  }

  private void validateKeyframeLimit(JsonNode animations) throws AnimationParseException {
    long total = 0;
    Iterator<Map.Entry<String, JsonNode>> clips = animations.fields();
    while (clips.hasNext()) {
      JsonNode clip = clips.next().getValue();
      if (clip == null || !clip.isObject()) continue;
      JsonNode bones = clip.get("bones");
      if (bones == null || !bones.isObject()) continue;
      Iterator<Map.Entry<String, JsonNode>> boneFields = bones.fields();
      while (boneFields.hasNext()) {
        JsonNode bone = boneFields.next().getValue();
        if (bone == null || !bone.isObject()) continue;
        total += sourceKeyframeCount(bone.get("position"));
        total += sourceKeyframeCount(bone.get("rotation"));
        total += sourceKeyframeCount(bone.get("scale"));
        if (total > limits.maxKeyframes()) {
          throw error(
              DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
              "animations contain more than " + limits.maxKeyframes() + " transform keyframes");
        }
      }
    }
  }

  private long sourceKeyframeCount(JsonNode channel) {
    if (channel == null || channel.isNull()) return 0;
    if (!channel.isObject()) return 1;

    long count = 0;
    Iterator<String> fields = channel.fieldNames();
    while (fields.hasNext()) {
      String field = fields.next();
      if (!"easing".equals(field) && !"easingArgs".equals(field) && !"lerp_mode".equals(field)) {
        count++;
      }
    }
    return count;
  }

  private AnimationClipIR parseClip(
      Path path,
      String clipName,
      JsonNode node,
      Map<String, BoneId> bonesByName,
      List<Diagnostic> diagnostics)
      throws AnimationParseException {
    if (clipName == null || clipName.isBlank() || node == null || !node.isObject()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "animation clip must be a named object");
    }

    List<BoneTrackIR> tracks = new ArrayList<>();
    double derivedDuration = 0;
    JsonNode bones = node.get("bones");
    if (bones != null && !bones.isNull()) {
      if (!bones.isObject()) {
        throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "clip bones must be an object");
      }
      Iterator<Map.Entry<String, JsonNode>> boneFields = bones.fields();
      while (boneFields.hasNext()) {
        Map.Entry<String, JsonNode> boneField = boneFields.next();
        BoneId boneId = bonesByName.get(boneField.getKey());
        if (boneId == null) {
          throw error(
              DiagnosticCodes.ANIM_BONE_NOT_FOUND,
              "animation " + clipName + " references unknown bone " + boneField.getKey());
        }
        JsonNode boneNode = boneField.getValue();
        if (boneNode == null || !boneNode.isObject()) {
          throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "animated bone must be an object");
        }

        String bonePointer = "/animations/" + clipName + "/bones/" + boneField.getKey();

        ChannelIR<Vec3d> position = null;
        JsonNode positionNode = boneNode.get("position");
        if (positionNode != null && !positionNode.isNull()) {
          position =
              parseVectorChannel(
                  "position",
                  positionNode,
                  POSITION_DEFAULT,
                  TransformMode.ADDITIVE,
                  bonePointer + "/position",
                  diagnostics);
          derivedDuration = Math.max(derivedDuration, maxChannelTime(position));
        }

        SourceRotationChannelIR rotation = null;
        JsonNode rotationNode = boneNode.get("rotation");
        if (rotationNode != null && !rotationNode.isNull()) {
          rotation =
              parseRotationChannel(
                  path,
                  clipName,
                  boneField.getKey(),
                  rotationNode,
                  bonePointer + "/rotation",
                  diagnostics);
          derivedDuration = Math.max(derivedDuration, maxRotationTime(rotation));
        }

        ChannelIR<Vec3d> scale = null;
        JsonNode scaleNode = boneNode.get("scale");
        if (scaleNode != null && !scaleNode.isNull()) {
          scale =
              parseVectorChannel(
                  "scale",
                  scaleNode,
                  SCALE_DEFAULT,
                  TransformMode.ABSOLUTE,
                  bonePointer + "/scale",
                  diagnostics);
          derivedDuration = Math.max(derivedDuration, maxChannelTime(scale));
        }

        tracks.add(
            new BoneTrackIR(
                boneId,
                position,
                rotation,
                scale,
                TransformMode.ADDITIVE,
                TransformSpace.LOCAL));
      }
    }

    double duration = explicitDuration(node.get("animation_length"), derivedDuration, clipName);
    Playback playback = playback(node.get("loop"));
    List<UnsupportedEventIR> events = parseUnsupportedEvents(path, clipName, node, diagnostics);
    return new AnimationClipIR(
        new ClipId(clipName), duration, playback.mode(), playback.customLoop(), tracks, events);
  }

  private ChannelIR<Vec3d> parseVectorChannel(
      String component,
      JsonNode node,
      Vec3d defaults,
      TransformMode mode,
      String pointer,
      List<Diagnostic> diagnostics)
      throws AnimationParseException {
    List<SourceVectorKeyframe> sourceFrames = new ArrayList<>();
    if (node.isNumber() || node.isTextual() || node.isArray()) {
      Vec3d value = channelValue(component, vector(node, defaults, pointer));
      sourceFrames.add(new SourceVectorKeyframe(0, value, LINEAR_EASING));
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (skipGecko449ChannelMetadata(field.getKey(), pointer, diagnostics)) continue;
        double time = timestamp(field.getKey(), pointer);
        String keyframePointer = pointer + "/" + field.getKey();
        Vec3d value =
            channelValue(
                component,
                effectiveGecko449KeyframeValue(
                    field.getValue(), defaults, keyframePointer, diagnostics));
        sourceFrames.add(
            new SourceVectorKeyframe(
                time, value, targetEasingMetadata(field.getValue(), keyframePointer)));
      }
      sourceFrames.sort(Comparator.comparingDouble(SourceVectorKeyframe::time));
    } else {
      throw error(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          component + " channel must be numeric at " + pointer);
    }
    if (sourceFrames.isEmpty()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, component + " channel is empty");
    }

    List<KeyframeIR<Vec3d>> keyframes = new ArrayList<>(sourceFrames.size());
    for (int i = 0; i < sourceFrames.size(); i++) {
      SourceVectorKeyframe current = sourceFrames.get(i);
      EasingMetadata after =
          i + 1 < sourceFrames.size() ? sourceFrames.get(i + 1).targetEasing() : LINEAR_EASING;
      keyframes.add(
          new KeyframeIR<>(
              current.time(),
              current.value(),
              current.value(),
              after.interpolation(),
              after.args()));
    }
    return new ChannelIR<>(component, mode, TransformSpace.LOCAL, keyframes);
  }

  private Vec3d channelValue(String component, Vec3d value) {
    return "position".equals(component) ? CoordinateBoundary.geckoToCpmPosition(value) : value;
  }

  private SourceRotationChannelIR parseRotationChannel(
      Path path,
      String clip,
      String bone,
      JsonNode node,
      String pointer,
      List<Diagnostic> diagnostics)
      throws AnimationParseException {
    List<SourceRotationFrame> sourceFrames = new ArrayList<>();
    if (node.isNumber() || node.isTextual() || node.isArray()) {
      Vec3d value = vector(node, Vec3d.ZERO, pointer);
      sourceFrames.add(
          new SourceRotationFrame(
              0, value, LINEAR_EASING, sourcePath(path) + "#" + pointer));
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (skipGecko449ChannelMetadata(field.getKey(), pointer, diagnostics)) continue;
        double time = timestamp(field.getKey(), pointer);
        String keyframePointer = pointer + "/" + field.getKey();
        Vec3d value =
            effectiveGecko449KeyframeValue(
                field.getValue(), Vec3d.ZERO, keyframePointer, diagnostics);
        sourceFrames.add(
            new SourceRotationFrame(
                time,
                value,
                targetEasingMetadata(field.getValue(), keyframePointer),
                sourcePath(path) + "#" + keyframePointer));
      }
      sourceFrames.sort(Comparator.comparingDouble(SourceRotationFrame::time));
    } else {
      throw error(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "rotation channel must be numeric at " + clip + "/" + bone + ": " + pointer);
    }
    if (sourceFrames.isEmpty()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "rotation channel is empty");
    }

    List<SourceRotationKeyframeIR> keyframes = new ArrayList<>(sourceFrames.size());
    for (int i = 0; i < sourceFrames.size(); i++) {
      SourceRotationFrame current = sourceFrames.get(i);
      EasingMetadata after =
          i + 1 < sourceFrames.size() ? sourceFrames.get(i + 1).targetEasing() : LINEAR_EASING;
      keyframes.add(
          new SourceRotationKeyframeIR(
              current.time(),
              current.value(),
              current.value(),
              after.interpolation(),
              after.args(),
              current.source()));
    }
    return new SourceRotationChannelIR(keyframes, RotationOrder.ZYX);
  }

  private boolean skipGecko449ChannelMetadata(
      String key, String pointer, List<Diagnostic> diagnostics) {
    if ("easing".equals(key) || "easingArgs".equals(key)) return true;
    if (!"lerp_mode".equals(key)) return false;
    diagnostics.add(
        Diagnostic.of(
            Severity.WARNING,
            DiagnosticCodes.ANIM_LERP_MODE_IGNORED_449,
            "GeckoLib 4.4.9 ignores channel lerp_mode at " + pointer + "/lerp_mode"));
    return true;
  }

  private EasingMetadata targetEasingMetadata(JsonNode node, String pointer)
      throws AnimationParseException {
    if (node == null || !node.isObject() || !node.has("vector")) return LINEAR_EASING;

    InterpolationIR interpolation = InterpolationIR.LINEAR;
    JsonNode easingNode = node.get("easing");
    if (easingNode != null && !easingNode.isNull()) {
      if (easingNode.isTextual()) {
        interpolation = easing(easingNode.textValue(), pointer + "/easing");
      }
    }

    List<Double> args = List.of();
    JsonNode argsNode = node.get("easingArgs");
    if (argsNode != null && !argsNode.isNull()) {
      if (!argsNode.isArray()) {
        throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "easingArgs must be an array at " + pointer);
      }
      List<Double> parsed = new ArrayList<>(argsNode.size());
      for (int i = 0; i < argsNode.size(); i++) {
        parsed.add(easingArgument(argsNode.get(i), pointer + "/easingArgs/" + i));
      }
      args = List.copyOf(parsed);
    }
    return new EasingMetadata(interpolation, args);
  }

  private InterpolationIR easing(String name, String pointer) throws AnimationParseException {
    String normalized = name.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "linear", "none" -> InterpolationIR.LINEAR;
      case "step" -> InterpolationIR.STEP;
      case "easeinsine" -> InterpolationIR.EASE_IN_SINE;
      case "easeoutsine" -> InterpolationIR.EASE_OUT_SINE;
      case "easeinoutsine" -> InterpolationIR.EASE_IN_OUT_SINE;
      case "easeinquad" -> InterpolationIR.EASE_IN_QUAD;
      case "easeoutquad" -> InterpolationIR.EASE_OUT_QUAD;
      case "easeinoutquad" -> InterpolationIR.EASE_IN_OUT_QUAD;
      case "easeincubic" -> InterpolationIR.EASE_IN_CUBIC;
      case "easeoutcubic" -> InterpolationIR.EASE_OUT_CUBIC;
      case "easeinoutcubic" -> InterpolationIR.EASE_IN_OUT_CUBIC;
      case "easeinquart" -> InterpolationIR.EASE_IN_QUART;
      case "easeoutquart" -> InterpolationIR.EASE_OUT_QUART;
      case "easeinoutquart" -> InterpolationIR.EASE_IN_OUT_QUART;
      case "easeinquint" -> InterpolationIR.EASE_IN_QUINT;
      case "easeoutquint" -> InterpolationIR.EASE_OUT_QUINT;
      case "easeinoutquint" -> InterpolationIR.EASE_IN_OUT_QUINT;
      case "easeinexpo" -> InterpolationIR.EASE_IN_EXPO;
      case "easeoutexpo" -> InterpolationIR.EASE_OUT_EXPO;
      case "easeinoutexpo" -> InterpolationIR.EASE_IN_OUT_EXPO;
      case "easeincirc" -> InterpolationIR.EASE_IN_CIRC;
      case "easeoutcirc" -> InterpolationIR.EASE_OUT_CIRC;
      case "easeinoutcirc" -> InterpolationIR.EASE_IN_OUT_CIRC;
      case "easeinback" -> InterpolationIR.EASE_IN_BACK;
      case "easeoutback" -> InterpolationIR.EASE_OUT_BACK;
      case "easeinoutback" -> InterpolationIR.EASE_IN_OUT_BACK;
      case "easeinelastic" -> InterpolationIR.EASE_IN_ELASTIC;
      case "easeoutelastic" -> InterpolationIR.EASE_OUT_ELASTIC;
      case "easeinoutelastic" -> InterpolationIR.EASE_IN_OUT_ELASTIC;
      case "easeinbounce" -> InterpolationIR.EASE_IN_BOUNCE;
      case "easeoutbounce" -> InterpolationIR.EASE_OUT_BOUNCE;
      case "easeinoutbounce" -> InterpolationIR.EASE_IN_OUT_BOUNCE;
      case "catmullrom" -> InterpolationIR.CATMULLROM;
      default ->
          throw error(
              DiagnosticCodes.ANIM_CUSTOM_EASING_UNSUPPORTED,
              "unknown or runtime-registered easing '" + name + "' at " + pointer);
    };
  }

  private double easingArgument(JsonNode node, String pointer) throws AnimationParseException {
    if (node != null && node.isNumber() && Double.isFinite(node.doubleValue())) {
      return node.doubleValue();
    }
    if (node != null && node.isTextual()) {
      try {
        double value = Double.parseDouble(node.textValue());
        if (Double.isFinite(value)) return value;
      } catch (NumberFormatException ignored) {
        // Fall through to the stable parse diagnostic below.
      }
    }
    throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "invalid easing argument at " + pointer);
  }

  private Vec3d effectiveGecko449KeyframeValue(
      JsonNode node, Vec3d defaults, String pointer, List<Diagnostic> diagnostics)
      throws AnimationParseException {
    if (node == null || node.isNull()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "missing keyframe value at " + pointer);
    }
    if (!node.isObject()) return vector(node, defaults, pointer);

    JsonNode vectorNode = node.get("vector");
    if (vectorNode != null && !vectorNode.isNull()) {
      return vector(vectorNode, defaults, pointer + "/vector");
    }

    JsonNode pre = node.get("pre");
    JsonNode post = node.get("post");
    boolean hasPre = pre != null && !pre.isNull();
    boolean hasPost = post != null && !post.isNull();
    if (!hasPre && !hasPost) {
      throw error(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "keyframe object has neither vector, pre nor post at " + pointer);
    }

    Vec3d selected =
        vector(hasPre ? pre : post, defaults, pointer + (hasPre ? "/pre" : "/post"));
    if (hasPre && hasPost) {
      Vec3d postValue = vector(post, defaults, pointer + "/post");
      if (!selected.equals(postValue)) {
        diagnostics.add(
            Diagnostic.of(
                Severity.WARNING,
                DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449,
                "GeckoLib 4.4.9 uses pre and discards the differing post value at " + pointer));
      }
    }
    return selected;
  }

  private List<UnsupportedEventIR> parseUnsupportedEvents(
      Path path, String clip, JsonNode clipNode, List<Diagnostic> diagnostics)
      throws AnimationParseException {
    List<UnsupportedEventIR> events = new ArrayList<>();
    parseUnsupportedEventCategory(path, clip, clipNode, "sound_effects", events, diagnostics);
    parseUnsupportedEventCategory(path, clip, clipNode, "particle_effects", events, diagnostics);
    parseUnsupportedEventCategory(path, clip, clipNode, "timeline", events, diagnostics);
    return List.copyOf(events);
  }

  private void parseUnsupportedEventCategory(
      Path path,
      String clip,
      JsonNode clipNode,
      String category,
      List<UnsupportedEventIR> events,
      List<Diagnostic> diagnostics)
      throws AnimationParseException {
    JsonNode eventNode = clipNode.get(category);
    if (eventNode == null || eventNode.isNull()) return;
    if (!eventNode.isObject()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, category + " must be an object");
    }

    Iterator<Map.Entry<String, JsonNode>> fields = eventNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      double time = timestamp(field.getKey(), "/animations/" + clip + "/" + category);
      String pointer = "/animations/" + clip + "/" + category + "/" + field.getKey();
      String message = category + " event at " + time + "s is outside the MVP conversion scope";
      events.add(
          new UnsupportedEventIR(
              DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE,
              message,
              sourcePath(path) + "#" + pointer));
      diagnostics.add(
          Diagnostic.of(Severity.WARNING, DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE, message));
    }
  }

  private Vec3d vector(JsonNode node, Vec3d defaults, String pointer)
      throws AnimationParseException {
    if (node == null || node.isNull()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "missing vector at " + pointer);
    }
    if (node.isNumber() || node.isTextual()) {
      double value = numericValue(node, pointer);
      return new Vec3d(value, value, value);
    }
    if (!node.isArray() || node.size() != 3) {
      throw error(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "expected scalar or three numeric values at " + pointer);
    }
    return new Vec3d(
        numericValue(node.get(0), pointer + "/0"),
        numericValue(node.get(1), pointer + "/1"),
        numericValue(node.get(2), pointer + "/2"));
  }

  private double numericValue(JsonNode node, String pointer) throws AnimationParseException {
    if (node != null && node.isNumber() && Double.isFinite(node.doubleValue())) {
      return node.doubleValue();
    }
    if (node != null && node.isTextual()) {
      try {
        return ConstantMolangEvaluator.evaluate(node.textValue());
      } catch (ConstantMolangEvaluator.MolangEvaluationException exception) {
        throw error(
            exception.dynamic()
                ? DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED
                : DiagnosticCodes.INPUT_PARSE_ERROR,
            exception.getMessage() + " at " + pointer);
      }
    }
    throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "expected finite number at " + pointer);
  }

  private double finiteNumber(JsonNode node, String pointer) throws AnimationParseException {
    if (node == null || !node.isNumber() || !Double.isFinite(node.doubleValue())) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "expected finite number at " + pointer);
    }
    return node.doubleValue();
  }

  private double timestamp(String value, String pointer) throws AnimationParseException {
    try {
      double timestamp = Double.parseDouble(value);
      if (!Double.isFinite(timestamp) || timestamp < 0) throw new NumberFormatException();
      return timestamp;
    } catch (NumberFormatException exception) {
      throw error(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "invalid animation timestamp " + value + " at " + pointer);
    }
  }

  private double maxChannelTime(ChannelIR<?> channel) {
    return channel.keyframes().stream().mapToDouble(KeyframeIR::time).max().orElse(0);
  }

  private double maxRotationTime(SourceRotationChannelIR channel) {
    return channel.keyframes().stream()
        .mapToDouble(SourceRotationKeyframeIR::timeSeconds)
        .max()
        .orElse(0);
  }

  private double explicitDuration(JsonNode node, double derived, String clip)
      throws AnimationParseException {
    if (node != null && !node.isNull()) {
      double duration = finiteNumber(node, "/animations/" + clip + "/animation_length");
      if (duration <= 0) {
        throw error(
            DiagnosticCodes.ANIM_ZERO_DURATION_INVALID, "animation_length must be positive");
      }
      return duration;
    }
    if (derived > 0 && Double.isFinite(derived)) return derived;
    throw error(
        DiagnosticCodes.ANIM_IMPLICIT_LENGTH_UNBOUNDED,
        "animation " + clip + " has no finite implicit duration");
  }

  private Playback playback(JsonNode node) throws AnimationParseException {
    if (node == null || node.isNull()) return new Playback(PlaybackMode.PLAY_ONCE, null);
    if (node.isBoolean()) {
      return new Playback(node.booleanValue() ? PlaybackMode.LOOP : PlaybackMode.PLAY_ONCE, null);
    }
    if (!node.isTextual()) {
      throw error(DiagnosticCodes.INPUT_PARSE_ERROR, "loop must be boolean or string");
    }
    String value = node.textValue();
    return switch (value) {
      case "true", "loop" -> new Playback(PlaybackMode.LOOP, null);
      case "false", "play_once" -> new Playback(PlaybackMode.PLAY_ONCE, null);
      case "hold_on_last_frame" -> new Playback(PlaybackMode.HOLD, null);
      default -> new Playback(PlaybackMode.CUSTOM, value);
    };
  }

  private String sourcePath(Path path) {
    Path fileName = path.getFileName();
    return fileName == null ? "animations.animation.json" : fileName.toString();
  }

  private AnimationParseException error(String code, String message) {
    return new AnimationParseException(code, message);
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }

  private record Playback(PlaybackMode mode, String customLoop) {}

  private record EasingMetadata(InterpolationIR interpolation, List<Double> args) {
    private EasingMetadata {
      args = List.copyOf(args);
    }
  }

  private record SourceVectorKeyframe(double time, Vec3d value, EasingMetadata targetEasing) {}

  private record SourceRotationFrame(
      double time, Vec3d value, EasingMetadata targetEasing, String source) {}

  private static final class AnimationParseException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String code;

    private AnimationParseException(String code, String message) {
      super(message);
      this.code = code;
    }

    private String code() {
      return code;
    }
  }
}
