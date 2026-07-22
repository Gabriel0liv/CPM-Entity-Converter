package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Validates relational ModelIR invariants and reports deterministic diagnostics. */
public final class ModelIrValidator {
  public DiagnosticBag validate(ModelIR model) {
    DiagnosticBag diagnostics = new DiagnosticBag();
    if (model == null) {
      return diagnostics.add(
          error(
              DiagnosticCodes.INTERNAL_ERROR,
              "model is null",
              Map.of(),
              null,
              null,
              SourceLocation.of(new SourcePath("<input>"))));
    }
    SourceLocation modelSource = SourceLocation.of(new SourcePath(model.source().path()));
    Map<BoneId, BoneIR> bones = new LinkedHashMap<>();
    for (BoneIR bone : model.bones()) {
      if (bones.putIfAbsent(bone.id(), bone) != null) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_DUPLICATE_BONE_ID,
                    "duplicate bone id",
                    Map.of("boneId", bone.id().value()),
                    bone.id().value(),
                    null,
                    bone.provenance()));
      }
    }
    Set<BoneId> roots = new LinkedHashSet<>();
    for (BoneId root : model.roots()) {
      if (!roots.add(root)) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_DUPLICATE,
                    "duplicate root",
                    Map.of("boneId", root.value()),
                    root.value(),
                    null,
                    modelSource));
      }
      BoneIR rootBone = bones.get(root);
      if (rootBone == null) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_MISSING,
                    "root not found",
                    Map.of("boneId", root.value()),
                    root.value(),
                    null,
                    modelSource));
      } else if (rootBone.parent() != null) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_PARENT,
                    "root has a parent",
                    Map.of("boneId", root.value(), "parentId", rootBone.parent().value()),
                    root.value(),
                    null,
                    rootBone.provenance()));
      }
    }
    for (BoneIR bone : model.bones()) {
      if (bone.parent() != null) {
        BoneIR parent = bones.get(bone.parent());
        if (parent == null) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_PARENT_MISSING,
                      "parent not found",
                      Map.of("boneId", bone.id().value(), "parentId", bone.parent().value()),
                      bone.id().value(),
                      null,
                      bone.provenance()));
        } else if (!parent.children().contains(bone.id())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_PARENT_CHILD_MISMATCH,
                      "parent does not list child",
                      Map.of("boneId", bone.id().value(), "parentId", bone.parent().value()),
                      bone.id().value(),
                      null,
                      bone.provenance()));
        }
      }
      Set<BoneId> children = new LinkedHashSet<>();
      for (BoneId child : bone.children()) {
        if (!children.add(child)) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CHILD_DUPLICATE,
                      "duplicate child",
                      Map.of("parentId", bone.id().value(), "childId", child.value()),
                      bone.id().value(),
                      null,
                      bone.provenance()));
        }
        BoneIR childBone = bones.get(child);
        if (childBone == null) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CHILD_MISSING,
                      "child not found",
                      Map.of("parentId", bone.id().value(), "childId", child.value()),
                      bone.id().value(),
                      null,
                      bone.provenance()));
        } else if (!bone.id().equals(childBone.parent())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_PARENT_CHILD_MISMATCH,
                      "child parent differs",
                      Map.of("parentId", bone.id().value(), "childId", child.value()),
                      bone.id().value(),
                      null,
                      bone.provenance()));
        }
      }
      for (CubeIR cube : bone.cubes()) {
        if (!bones.containsKey(cube.bone())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CUBE_BONE_MISSING,
                      "cube bone not found",
                      Map.of("cubeId", cube.id().value(), "boneId", cube.bone().value()),
                      bone.id().value(),
                      null,
                      cube.provenance()));
        }
      }
    }
    Set<BoneId> done = new HashSet<>();
    for (BoneIR bone : model.bones()) {
      if (hasCycle(bone.id(), bones, new HashSet<>(), done)) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_CYCLE,
                    "bone graph contains a cycle",
                    Map.of("boneId", bone.id().value()),
                    bone.id().value(),
                    null,
                    bone.provenance()));
      }
      if (!reachableFromRoots(bone.id(), bones, roots)) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_UNREACHABLE_BONE,
                    "bone is unreachable from roots",
                    Map.of("boneId", bone.id().value()),
                    bone.id().value(),
                    null,
                    bone.provenance()));
      }
    }
    Set<CubeId> cubes = new LinkedHashSet<>();
    for (BoneIR bone : model.bones()) {
      for (CubeIR cube : bone.cubes()) {
        if (!cubes.add(cube.id())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_DUPLICATE_CUBE_ID,
                      "duplicate cube id",
                      Map.of("cubeId", cube.id().value()),
                      bone.id().value(),
                      null,
                      cube.provenance()));
        }
      }
    }
    Set<ClipId> clips = new LinkedHashSet<>();
    for (AnimationClipIR clip : model.clips()) {
      if (!clips.add(clip.id())) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_DUPLICATE_CLIP_ID,
                    "duplicate clip id",
                    Map.of("clipId", clip.id().value()),
                    null,
                    clip.id().value(),
                    clip.source()));
      }
      if (!Double.isFinite(clip.duration()) || clip.duration() <= 0) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_DURATION_INVALID,
                    "duration must be positive and finite",
                    Map.of(
                        "clipId", clip.id().value(), "duration", Double.toString(clip.duration())),
                    null,
                    clip.id().value(),
                    clip.source()));
      }
      if (clip.playback() == PlaybackMode.CUSTOM
          && (clip.customLoop() == null || clip.customLoop().isBlank())) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_CUSTOM_PLAYBACK_ID,
                    "custom playback requires source id",
                    Map.of("clipId", clip.id().value()),
                    null,
                    clip.id().value(),
                    clip.source()));
      }
      for (BoneTrackIR track : clip.tracks()) {
        if (!bones.containsKey(track.bone())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_TRACK_BONE_MISSING,
                      "track bone not found",
                      Map.of("clipId", clip.id().value(), "boneId", track.bone().value()),
                      track.bone().value(),
                      clip.id().value(),
                      track.source()));
        }
        diagnostics = validateChannel(track.position(), clip.duration(), diagnostics, clip, track);
        diagnostics = validateRotation(track.rotation(), clip.duration(), diagnostics, clip, track);
        diagnostics = validateChannel(track.scale(), clip.duration(), diagnostics, clip, track);
      }
    }
    return diagnostics;
  }

  private DiagnosticBag validateChannel(
      ChannelIR<?> channel,
      double duration,
      DiagnosticBag diagnostics,
      AnimationClipIR clip,
      BoneTrackIR track) {
    if (channel == null) return diagnostics;
    double previous = Double.NEGATIVE_INFINITY;
    Set<Double> seen = new HashSet<>();
    for (KeyframeIR<?> keyframe : channel.keyframes()) {
      double time = keyframe.time();
      if (!Double.isFinite(time) || time < 0) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_TIMESTAMP_INVALID,
                    "timestamp must be finite and non-negative",
                    Map.of(
                        "timestamp", Double.toString(time), "duration", Double.toString(duration)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
        continue;
      }
      if (!seen.add(time)) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_DUPLICATE,
                    "duplicate timestamp in channel",
                    Map.of("timestamp", Double.toString(time)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      if (time < previous) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_ORDER,
                    "timestamps must be non-decreasing",
                    Map.of("timestamp", Double.toString(time)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      if (time > duration) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION,
                    "timestamp exceeds clip duration",
                    Map.of(
                        "timestamp", Double.toString(time), "duration", Double.toString(duration)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      previous = time;
    }
    return diagnostics;
  }

  private DiagnosticBag validateRotation(
      SourceRotationChannelIR channel,
      double duration,
      DiagnosticBag diagnostics,
      AnimationClipIR clip,
      BoneTrackIR track) {
    if (channel == null) return diagnostics;
    double previous = Double.NEGATIVE_INFINITY;
    Set<Double> seen = new HashSet<>();
    for (SourceRotationKeyframeIR keyframe : channel.keyframes()) {
      double time = keyframe.timeSeconds();
      if (!Double.isFinite(time) || time < 0) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_TIMESTAMP_INVALID,
                    "rotation timestamp must be finite and non-negative",
                    Map.of(
                        "timestamp", Double.toString(time), "duration", Double.toString(duration)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
        continue;
      }
      if (!seen.add(time)) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_DUPLICATE,
                    "duplicate rotation timestamp in channel",
                    Map.of("timestamp", Double.toString(time)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      if (time < previous) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_ORDER,
                    "rotation timestamps must be non-decreasing",
                    Map.of("timestamp", Double.toString(time)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      if (time > duration) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION,
                    "rotation timestamp exceeds clip duration",
                    Map.of(
                        "timestamp", Double.toString(time), "duration", Double.toString(duration)),
                    track.bone().value(),
                    clip.id().value(),
                    keyframe.source()));
      }
      previous = time;
    }
    return diagnostics;
  }

  private boolean hasCycle(
      BoneId id, Map<BoneId, BoneIR> bones, Set<BoneId> active, Set<BoneId> done) {
    if (done.contains(id)) return false;
    if (!active.add(id)) return true;
    BoneIR bone = bones.get(id);
    boolean cycle =
        bone != null && bone.parent() != null && hasCycle(bone.parent(), bones, active, done);
    active.remove(id);
    done.add(id);
    return cycle;
  }

  private boolean reachableFromRoots(BoneId id, Map<BoneId, BoneIR> bones, Set<BoneId> roots) {
    Set<BoneId> seen = new HashSet<>();
    BoneId current = id;
    while (current != null && seen.add(current)) {
      if (roots.contains(current)) return true;
      BoneIR bone = bones.get(current);
      current = bone == null ? null : bone.parent();
    }
    return false;
  }

  private Diagnostic error(
      String code,
      String message,
      Map<String, String> context,
      String bone,
      String animation,
      SourceLocation location) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        location,
        message,
        suggestionFor(code),
        bone,
        animation,
        new TreeMap<>(context));
  }

  private String suggestionFor(String code) {
    return switch (code) {
      case DiagnosticCodes.IR_KEYFRAME_DUPLICATE -> "remove the duplicate timestamp";
      case DiagnosticCodes.IR_KEYFRAME_ORDER -> "sort keyframes by time";
      case DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION ->
          "increase clip duration or move the keyframe";
      case DiagnosticCodes.IR_TIMESTAMP_INVALID -> "use a finite non-negative timestamp";
      case DiagnosticCodes.IR_DURATION_INVALID -> "use a finite positive duration";
      case DiagnosticCodes.IR_PARENT_MISSING -> "declare the missing parent";
      case DiagnosticCodes.IR_CHILD_MISSING -> "declare the missing child";
      case DiagnosticCodes.IR_CYCLE -> "remove the parent cycle";
      case DiagnosticCodes.IR_CUSTOM_PLAYBACK_ID -> "provide a custom playback identifier";
      default -> "correct the ModelIR input";
    };
  }
}
