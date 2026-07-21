package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validates graph, identity and timeline invariants without throwing generic errors. */
public final class ModelIrValidator {
    public DiagnosticBag validate(ModelIR model) {
        DiagnosticBag diagnostics = new DiagnosticBag();
        if (model == null) {
            return diagnostics.add(error(DiagnosticCodes.INTERNAL_ERROR, "model is null"));
        }

        Map<BoneId, BoneIR> bones = new HashMap<>();
        for (BoneIR bone : model.bones()) {
            if (bones.putIfAbsent(bone.id(), bone) != null) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_DUPLICATE_BONE_ID, "duplicate bone id"));
            }
        }

        Set<BoneId> roots = new HashSet<>();
        for (BoneId root : model.roots()) {
            if (!roots.add(root)) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_ROOT_DUPLICATE, "duplicate root"));
            }
            BoneIR rootBone = bones.get(root);
            if (rootBone == null) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_ROOT_MISSING, "root not found"));
            } else if (rootBone.parent() != null) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_ROOT_PARENT, "root has a parent"));
            }
        }

        for (BoneIR bone : model.bones()) {
            if (bone.parent() != null) {
                BoneIR parent = bones.get(bone.parent());
                if (parent == null) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_PARENT_MISSING, "parent not found"));
                } else if (!parent.children().contains(bone.id())) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_PARENT_CHILD_MISMATCH, "parent does not list child"));
                }
            }
            Set<BoneId> children = new HashSet<>();
            for (BoneId child : bone.children()) {
                if (!children.add(child)) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CHILD_DUPLICATE, "duplicate child"));
                }
                BoneIR childBone = bones.get(child);
                if (childBone == null) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CHILD_MISSING, "child not found"));
                } else if (!bone.id().equals(childBone.parent())) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_PARENT_CHILD_MISMATCH, "child parent differs"));
                }
            }
            for (CubeIR cube : bone.cubes()) {
                if (!bones.containsKey(cube.bone())) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CUBE_BONE_MISSING, "cube bone not found"));
                }
            }
        }

        for (BoneIR bone : model.bones()) {
            if (hasCycle(bone.id(), bones, new HashSet<>(), new HashSet<>())) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CYCLE, "bone graph contains a cycle"));
                break;
            }
        }
        for (BoneIR bone : model.bones()) {
            if (!reachableFromRoots(bone.id(), bones, roots)) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_UNREACHABLE_BONE, "bone is unreachable from roots"));
            }
        }

        Set<CubeId> cubeIds = new HashSet<>();
        for (BoneIR bone : model.bones()) {
            for (CubeIR cube : bone.cubes()) {
                if (!cubeIds.add(cube.id())) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_DUPLICATE_CUBE_ID, "duplicate cube id"));
                }
            }
        }
        Set<ClipId> clipIds = new HashSet<>();
        for (AnimationClipIR clip : model.clips()) {
            if (!clipIds.add(clip.id())) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_DUPLICATE_CLIP_ID, "duplicate clip id"));
            }
            if (!Double.isFinite(clip.duration()) || clip.duration() <= 0) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_DURATION_INVALID, "duration must be positive and finite"));
            }
            if (clip.playback() == PlaybackMode.CUSTOM && (clip.customLoop() == null || clip.customLoop().isBlank())) {
                diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CUSTOM_PLAYBACK_ID, "custom playback requires source id"));
            }
            for (BoneTrackIR track : clip.tracks()) {
                if (!bones.containsKey(track.bone())) {
                    diagnostics = diagnostics.add(error(DiagnosticCodes.IR_TRACK_BONE_MISSING, "track bone not found"));
                }
                diagnostics = validateChannel(track.position(), clip.duration(), diagnostics);
                diagnostics = validateRotation(track.rotation(), clip.duration(), diagnostics);
                diagnostics = validateChannel(track.scale(), clip.duration(), diagnostics);
            }
        }
        return diagnostics;
    }

    private DiagnosticBag validateChannel(ChannelIR<?> channel, double duration, DiagnosticBag diagnostics) {
        if (channel == null) return diagnostics;
        double previous = -1;
        Set<Double> seen = new HashSet<>();
        for (KeyframeIR<?> keyframe : channel.keyframes()) {
            if (!seen.add(keyframe.time())) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_DUPLICATE, "duplicate timestamp"));
            if (keyframe.time() < previous) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_ORDER, "timestamps are not ordered"));
            if (keyframe.time() > duration) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION, "timestamp exceeds clip duration"));
            previous = keyframe.time();
        }
        return diagnostics;
    }

    private DiagnosticBag validateRotation(SourceRotationChannelIR channel, double duration, DiagnosticBag diagnostics) {
        if (channel == null) return diagnostics;
        double previous = -1;
        Set<Double> seen = new HashSet<>();
        for (SourceRotationKeyframeIR keyframe : channel.keyframes()) {
            if (!seen.add(keyframe.timeSeconds())) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_DUPLICATE, "duplicate rotation timestamp"));
            if (keyframe.timeSeconds() < previous) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_ORDER, "rotation timestamps are not ordered"));
            if (keyframe.timeSeconds() > duration) diagnostics = diagnostics.add(error(DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION, "rotation timestamp exceeds clip duration"));
            previous = keyframe.timeSeconds();
        }
        return diagnostics;
    }

    private boolean hasCycle(BoneId id, Map<BoneId, BoneIR> bones, Set<BoneId> active, Set<BoneId> done) {
        if (done.contains(id)) return false;
        if (!active.add(id)) return true;
        BoneIR bone = bones.get(id);
        boolean cycle = bone != null && bone.parent() != null && hasCycle(bone.parent(), bones, active, done);
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

    private Diagnostic error(String code, String message) {
        return Diagnostic.of(Severity.ERROR, code, message);
    }
}
