package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoneTrackIR;
import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.RotationContinuityIR;
import io.github.gabriel0liv.cpmconverter.ir.SampledTransformIR;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AnimationSampler {
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  public Result<SampledClipIR> sample(
      AnimationClipIR clip,
      List<BoneId> canonicalBoneOrder,
      SamplingRequest request,
      InterpolationEvaluator evaluator) {
    Result<Void> requestValidation =
        validateRequest(clip, canonicalBoneOrder, request, evaluator);
    if (!requestValidation.success()) return Result.failure(requestValidation.diagnostics());

    Result<Void> playbackValidation = validatePlayback(clip.playback(), request.timelineKind());
    if (!playbackValidation.success()) return Result.failure(playbackValidation.diagnostics());

    Result<Map<BoneId, BoneTrackIR>> indexedTracks =
        indexTracks(clip.tracks(), canonicalBoneOrder);
    if (!indexedTracks.success()) return Result.failure(indexedTracks.diagnostics());

    TimelineGrid.Result grid =
        TimelineGrid.build(clip.duration(), request.requestedFps(), request.timelineKind());
    List<SampledFrameIR> frames = new ArrayList<>(grid.times().size());

    for (int frameIndex = 0; frameIndex < grid.times().size(); frameIndex++) {
      double timeSeconds = grid.times().get(frameIndex);
      List<SampledBoneTransformIR> bones = new ArrayList<>(canonicalBoneOrder.size());

      for (BoneId boneId : canonicalBoneOrder) {
        BoneTrackIR track = indexedTracks.value().get(boneId);
        if (track == null) {
          bones.add(identityBone(boneId));
          continue;
        }

        Result<Vec3d> position =
            VectorChannelSampler.sample(track.position(), timeSeconds, Vec3d.ZERO, evaluator);
        Result<RotationChannelSampler.RotationSample> rotation =
            RotationChannelSampler.sample(track.rotation(), timeSeconds, evaluator);
        Result<Vec3d> scale =
            VectorChannelSampler.sample(track.scale(), timeSeconds, ONE, evaluator);

        DiagnosticBag diagnostics =
            position
                .diagnostics()
                .addAll(rotation.diagnostics())
                .addAll(scale.diagnostics());
        if (diagnostics.hasErrors()) return Result.failure(diagnostics);

        SampledTransformIR transform =
            new SampledTransformIR(
                position.value(),
                rotation.value().rotation(),
                scale.value(),
                rotation.value().continuity());
        bones.add(
            new SampledBoneTransformIR(
                boneId,
                transform,
                Optional.of(new TrackSemanticsIR(track.mode(), track.space()))));
      }

      frames.add(new SampledFrameIR(frameIndex, timeSeconds, bones));
    }

    SampledClipKey key =
        new SampledClipKey(request.clipId(), request.requestedFps(), request.timelineKind());
    return Result.success(
        new SampledClipIR(key, clip.duration(), clip.playback(), grid.metadata(), frames));
  }

  private static Result<Void> validateRequest(
      AnimationClipIR clip,
      List<BoneId> canonicalBoneOrder,
      SamplingRequest request,
      InterpolationEvaluator evaluator) {
    if (clip == null || canonicalBoneOrder == null || request == null || evaluator == null) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
          "sampling request requires clip, canonical bones, request, and evaluator");
    }
    if (!clip.id().equals(request.clipId())) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
          "sampling request clip id does not match the source clip");
    }

    Set<BoneId> seen = new HashSet<>();
    for (BoneId bone : canonicalBoneOrder) {
      if (bone == null || !seen.add(bone)) {
        return failure(
            DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
            "canonical bone order contains null or duplicate bones");
      }
    }
    return successVoid();
  }

  private static Result<Void> validatePlayback(PlaybackMode playback, TimelineKind kind) {
    TimelineKind required =
        switch (playback) {
          case LOOP -> TimelineKind.LOOP;
          case PLAY_ONCE, HOLD -> TimelineKind.SINGLE;
          case CUSTOM -> null;
        };
    if (required == null || kind != required) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_PLAYBACK_UNSUPPORTED,
          "source playback is incompatible with the requested timeline kind");
    }
    return successVoid();
  }

  private static Result<Map<BoneId, BoneTrackIR>> indexTracks(
      List<BoneTrackIR> tracks, List<BoneId> canonicalBoneOrder) {
    Set<BoneId> canonical = new HashSet<>(canonicalBoneOrder);
    Map<BoneId, BoneTrackIR> indexed = new HashMap<>();
    for (BoneTrackIR track : tracks) {
      if (track == null || !canonical.contains(track.bone())) {
        return failure(
            DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
            "animation track references a bone outside the canonical model order");
      }
      if (indexed.putIfAbsent(track.bone(), track) != null) {
        return failure(
            DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID,
            "animation clip contains duplicate tracks for a bone");
      }
    }
    return Result.success(Map.copyOf(indexed));
  }

  private static SampledBoneTransformIR identityBone(BoneId boneId) {
    SampledTransformIR identity =
        new SampledTransformIR(Vec3d.ZERO, Quatd.IDENTITY, ONE, new RotationContinuityIR(true));
    return new SampledBoneTransformIR(boneId, identity, Optional.empty());
  }

  private static <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }

  private static Result<Void> successVoid() {
    return Result.success(VoidValue.INSTANCE).map(ignored -> null);
  }

  private enum VoidValue {
    INSTANCE
  }
}
