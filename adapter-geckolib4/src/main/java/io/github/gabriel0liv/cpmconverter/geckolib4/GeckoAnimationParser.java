package io.github.gabriel0liv.cpmconverter.geckolib4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoneTrackIR;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.RotationOrder;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationKeyframeIR;
import io.github.gabriel0liv.cpmconverter.ir.TransformMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformSpace;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline parser for the numeric GeckoLib 4.4.9 animation syntax used by the MVP fixtures. */
public final class GeckoAnimationParser {
  private final ObjectMapper json = new ObjectMapper();

  public Result<List<AnimationClipIR>> parse(Path path, ModelIR model) {
    try {
      if (path == null) return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animation path is null");
      if (model == null) return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "model is null");

      JsonNode root = json.readTree(Files.readString(path));
      if (root == null || !root.isObject()) {
        return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animation root must be an object");
      }
      JsonNode animations = root.get("animations");
      if (animations == null || !animations.isObject()) {
        return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "animations object is missing");
      }

      Map<String, BoneId> bonesByName = new LinkedHashMap<>();
      model.bones().forEach(bone -> bonesByName.put(bone.name(), bone.id()));

      List<AnimationClipIR> clips = new ArrayList<>();
      Iterator<Map.Entry<String, JsonNode>> fields = animations.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        clips.add(parseClip(path, field.getKey(), field.getValue(), bonesByName));
      }
      return Result.success(List.copyOf(clips));
    } catch (AnimationParseException exception) {
      return failure(exception.code(), exception.getMessage());
    } catch (Exception exception) {
      return failure(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          exception.getMessage() == null ? "cannot parse animations" : exception.getMessage());
    }
  }

  private AnimationClipIR parseClip(
      Path path, String clipName, JsonNode node, Map<String, BoneId> bonesByName)
      throws AnimationParseException {
    if (clipName == null || clipName.isBlank() || node == null || !node.isObject()) {
      throw error("ANIM_INVALID_VALUE", "animation clip must be a named object");
    }

    List<BoneTrackIR> tracks = new ArrayList<>();
    double derivedDuration = 0;
    JsonNode bones = node.get("bones");
    if (bones != null && !bones.isNull()) {
      if (!bones.isObject()) throw error("ANIM_INVALID_VALUE", "clip bones must be an object");
      Iterator<Map.Entry<String, JsonNode>> boneFields = bones.fields();
      while (boneFields.hasNext()) {
        Map.Entry<String, JsonNode> boneField = boneFields.next();
        BoneId boneId = bonesByName.get(boneField.getKey());
        if (boneId == null) {
          throw error(
              "ANIM_BONE_MISSING",
              "animation " + clipName + " references unknown bone " + boneField.getKey());
        }
        JsonNode boneNode = boneField.getValue();
        if (boneNode == null || !boneNode.isObject()) {
          throw error("ANIM_INVALID_VALUE", "animated bone must be an object");
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
                  "/animations/" + clipName + "/bones/" + boneField.getKey() + "/rotation");
          derivedDuration = Math.max(derivedDuration, maxRotationTime(rotation));
        }

        tracks.add(
            new BoneTrackIR(
                boneId,
                null,
                rotation,
                null,
                TransformMode.ADDITIVE,
                TransformSpace.LOCAL));
      }
    }

    double duration = explicitDuration(node.get("animation_length"), derivedDuration, clipName);
    Playback playback = playback(node.get("loop"));
    return new AnimationClipIR(
        new ClipId(clipName), duration, playback.mode(), playback.customLoop(), tracks, List.of());
  }

  private SourceRotationChannelIR parseRotationChannel(
      Path path, String clip, String bone, JsonNode node, String pointer)
      throws AnimationParseException {
    List<SourceRotationKeyframeIR> keyframes = new ArrayList<>();
    if (node.isNumber() || node.isArray()) {
      Vec3d value = vector(node, Vec3d.ZERO, pointer);
      keyframes.add(rotationKeyframe(path, pointer, 0, value));
    } else if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        double time = timestamp(field.getKey(), pointer);
        Vec3d value = vector(field.getValue(), Vec3d.ZERO, pointer + "/" + field.getKey());
        keyframes.add(rotationKeyframe(path, pointer + "/" + field.getKey(), time, value));
      }
      keyframes.sort(Comparator.comparingDouble(SourceRotationKeyframeIR::timeSeconds));
    } else {
      throw error(
          "ANIM_INVALID_VALUE",
          "rotation channel must be numeric at " + clip + "/" + bone + ": " + pointer);
    }
    if (keyframes.isEmpty()) throw error("ANIM_INVALID_VALUE", "rotation channel is empty");
    return new SourceRotationChannelIR(keyframes, RotationOrder.ZYX);
  }

  private SourceRotationKeyframeIR rotationKeyframe(
      Path path, String pointer, double time, Vec3d value) {
    return new SourceRotationKeyframeIR(
        time, value, value, InterpolationIR.LINEAR, sourcePath(path) + "#" + pointer);
  }

  private Vec3d vector(JsonNode node, Vec3d defaults, String pointer)
      throws AnimationParseException {
    if (node == null || node.isNull()) throw error("ANIM_INVALID_VALUE", "missing vector at " + pointer);
    if (node.isNumber()) {
      double value = finiteNumber(node, pointer);
      return new Vec3d(value, value, value);
    }
    if (!node.isArray() || node.isEmpty() || node.size() > 3) {
      throw error("ANIM_INVALID_VALUE", "expected one to three numeric values at " + pointer);
    }
    if (node.size() == 1) {
      double value = finiteNumber(node.get(0), pointer);
      return new Vec3d(value, value, value);
    }
    double x = finiteNumber(node.get(0), pointer);
    double y = finiteNumber(node.get(1), pointer);
    double z = node.size() == 3 ? finiteNumber(node.get(2), pointer) : defaults.z();
    return new Vec3d(x, y, z);
  }

  private double finiteNumber(JsonNode node, String pointer) throws AnimationParseException {
    if (node == null || !node.isNumber() || !Double.isFinite(node.doubleValue())) {
      throw error("ANIM_INVALID_VALUE", "expected finite number at " + pointer);
    }
    return node.doubleValue();
  }

  private double timestamp(String value, String pointer) throws AnimationParseException {
    try {
      double timestamp = Double.parseDouble(value);
      if (!Double.isFinite(timestamp) || timestamp < 0) throw new NumberFormatException();
      return timestamp;
    } catch (NumberFormatException exception) {
      throw error("ANIM_INVALID_VALUE", "invalid animation timestamp " + value + " at " + pointer);
    }
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
      if (duration <= 0) throw error("ANIM_INVALID_VALUE", "animation_length must be positive");
      return duration;
    }
    if (derived > 0 && Double.isFinite(derived)) return derived;
    throw error(
        "ANIM_IMPLICIT_LENGTH_UNBOUNDED",
        "animation " + clip + " has no finite implicit duration");
  }

  private Playback playback(JsonNode node) throws AnimationParseException {
    if (node == null || node.isNull()) return new Playback(PlaybackMode.PLAY_ONCE, null);
    if (node.isBoolean()) {
      return new Playback(node.booleanValue() ? PlaybackMode.LOOP : PlaybackMode.PLAY_ONCE, null);
    }
    if (!node.isTextual()) {
      throw error("ANIM_INVALID_VALUE", "loop must be boolean or string");
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
