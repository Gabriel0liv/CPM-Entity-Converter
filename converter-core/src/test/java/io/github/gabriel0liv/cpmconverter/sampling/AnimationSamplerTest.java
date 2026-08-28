package io.github.gabriel0liv.cpmconverter.sampling;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoneTrackIR;
import io.github.gabriel0liv.cpmconverter.ir.ChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import io.github.gabriel0liv.cpmconverter.ir.InterpolationIR;
import io.github.gabriel0liv.cpmconverter.ir.KeyframeIR;
import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformSpace;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimationSamplerTest {
  private static final ClipId CLIP = new ClipId("test.clip");
  private static final BoneId BODY = new BoneId("body");
  private static final BoneId HEAD = new BoneId("head");
  private static final BoneId HORN = new BoneId("horn");
  private static final Vec3d ONE = new Vec3d(1, 1, 1);
  private static final InterpolationEvaluator LINEAR_EVALUATOR =
      (interpolation, args, progress) -> progress;

  private final AnimationSampler sampler = new AnimationSampler();

  @Test
  void everyFrameContainsEveryCanonicalBoneInOrder() {
    var order = List.of(BODY, HEAD, HORN);
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));

    var result = sampler.sample(clip, order, request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertTrue(result.success());
    for (var frame : result.value().frames()) {
      assertEquals(order, frame.bones().stream().map(SampledBoneTransformIR::bone).toList());
    }
  }

  @Test
  void boneWithoutTrackHasIdentityAndNoInventedTrackSemantics() {
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));

    var result =
        sampler.sample(
            clip, List.of(BODY, HEAD), request(2, TimelineKind.LOOP), LINEAR_EVALUATOR);

    var body = sampledBone(result.value(), 0, BODY);
    assertEquals(Vec3d.ZERO, body.transform().translation());
    assertEquals(Quatd.IDENTITY, body.transform().rotation());
    assertEquals(ONE, body.transform().scale());
    assertTrue(body.trackSemantics().isEmpty());
  }

  @Test
  void animatedBoneWithMissingChannelsFillsOnlyThoseChannelsWithIdentity() {
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));

    var result =
        sampler.sample(clip, List.of(HEAD), request(2, TimelineKind.LOOP), LINEAR_EVALUATOR);

    var head = sampledBone(result.value(), 1, HEAD);
    assertEquals(new Vec3d(5, 0, 0), head.transform().translation());
    assertEquals(Quatd.IDENTITY, head.transform().rotation());
    assertEquals(ONE, head.transform().scale());
    assertEquals(
        new TrackSemanticsIR(TransformMode.ADDITIVE, TransformSpace.LOCAL),
        head.trackSemantics().orElseThrow());
  }

  @Test
  void shuffledTrackInputDoesNotChangeSampledOutput() {
    var head = positionTrack(HEAD, 0, 10);
    var horn = positionTrack(HORN, 4, 8);
    var order = List.of(BODY, HEAD, HORN);
    var first = clip(PlaybackMode.LOOP, List.of(head, horn));
    var second = clip(PlaybackMode.LOOP, List.of(horn, head));

    var a = sampler.sample(first, order, request(4, TimelineKind.LOOP), LINEAR_EVALUATOR);
    var b = sampler.sample(second, order, request(4, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertTrue(a.success());
    assertTrue(b.success());
    assertEquals(a.value(), b.value());
  }

  @Test
  void sameClipAt20And40FpsProducesDistinctKeysAndFrameCounts() {
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));

    var a =
        sampler.sample(
            clip, List.of(HEAD), request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);
    var b =
        sampler.sample(
            clip, List.of(HEAD), request(40, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertTrue(a.success());
    assertTrue(b.success());
    assertNotEquals(a.value().key(), b.value().key());
    assertEquals(20, a.value().metadata().frameCount());
    assertEquals(40, b.value().metadata().frameCount());
  }

  @Test
  void sameClipIdAndRateWithLoopAndSingleAreNotConflated() {
    var loop = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));
    var single = clip(PlaybackMode.PLAY_ONCE, List.of(positionTrack(HEAD, 0, 10)));

    var a =
        sampler.sample(
            loop, List.of(HEAD), request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);
    var b =
        sampler.sample(
            single, List.of(HEAD), request(20, TimelineKind.SINGLE), LINEAR_EVALUATOR);

    assertTrue(a.success());
    assertTrue(b.success());
    assertNotEquals(a.value().key(), b.value().key());
  }

  @Test
  void holdPlaybackUsesSingleTimelineClassification() {
    var hold = clip(PlaybackMode.HOLD, List.of(positionTrack(HEAD, 0, 10)));

    var result =
        sampler.sample(
            hold, List.of(HEAD), request(2, TimelineKind.SINGLE), LINEAR_EVALUATOR);

    assertTrue(result.success());
    assertEquals(0.0, result.value().frames().get(0).timeSeconds(), 0.0);
    assertEquals(1.0, result.value().frames().get(1).timeSeconds(), 0.0);
  }

  @Test
  void requestClipMismatchReturnsStableRequestDiagnostic() {
    var clip = clip(PlaybackMode.LOOP, List.of());
    var request = new SamplingRequest(new ClipId("other.clip"), 20, TimelineKind.LOOP);

    var result = sampler.sample(clip, List.of(BODY), request, LINEAR_EVALUATOR);

    assertDiagnostic(result, DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID);
  }

  @Test
  void incompatibleOrCustomPlaybackReturnsStablePlaybackDiagnostic() {
    var loop = clip(PlaybackMode.LOOP, List.of());
    var custom =
        new AnimationClipIR(CLIP, 1.0, PlaybackMode.CUSTOM, "custom_loop", List.of());

    var incompatible =
        sampler.sample(
            loop, List.of(BODY), request(20, TimelineKind.SINGLE), LINEAR_EVALUATOR);
    var unsupported =
        sampler.sample(
            custom, List.of(BODY), request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertDiagnostic(incompatible, DiagnosticCodes.ANIM_SAMPLING_PLAYBACK_UNSUPPORTED);
    assertDiagnostic(unsupported, DiagnosticCodes.ANIM_SAMPLING_PLAYBACK_UNSUPPORTED);
  }

  @Test
  void duplicateTrackBoneReturnsStableRequestDiagnostic() {
    var first = positionTrack(HEAD, 0, 10);
    var second = positionTrack(HEAD, 10, 20);
    var clip = clip(PlaybackMode.LOOP, List.of(first, second));

    var result =
        sampler.sample(clip, List.of(HEAD), request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertDiagnostic(result, DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID);
  }

  @Test
  void trackOutsideCanonicalBoneOrderReturnsStableRequestDiagnostic() {
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HORN, 0, 10)));

    var result =
        sampler.sample(
            clip, List.of(BODY, HEAD), request(20, TimelineKind.LOOP), LINEAR_EVALUATOR);

    assertDiagnostic(result, DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID);
  }

  @Test
  void nullEvaluatorReturnsStableRequestDiagnostic() {
    var clip = clip(PlaybackMode.LOOP, List.of());

    var result = sampler.sample(clip, List.of(BODY), request(20, TimelineKind.LOOP), null);

    assertDiagnostic(result, DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID);
  }

  @Test
  void childSamplingFailureIsPropagatedWithoutInventingAFrame() {
    var clip = clip(PlaybackMode.LOOP, List.of(positionTrack(HEAD, 0, 10)));
    InterpolationEvaluator invalid = (interpolation, args, progress) -> Double.NaN;

    var result = sampler.sample(clip, List.of(HEAD), request(2, TimelineKind.LOOP), invalid);

    assertDiagnostic(result, DiagnosticCodes.ANIM_SAMPLING_NON_FINITE);
  }

  private static AnimationClipIR clip(PlaybackMode playback, List<BoneTrackIR> tracks) {
    return new AnimationClipIR(CLIP, 1.0, playback, null, tracks);
  }

  private static SamplingRequest request(int fps, TimelineKind kind) {
    return new SamplingRequest(CLIP, fps, kind);
  }

  private static BoneTrackIR positionTrack(BoneId bone, double from, double to) {
    var position =
        new ChannelIR<>(
            "position",
            TransformMode.ADDITIVE,
            TransformSpace.LOCAL,
            List.of(
                new KeyframeIR<>(
                    0.0,
                    new Vec3d(from, 0, 0),
                    new Vec3d(from, 0, 0),
                    InterpolationIR.LINEAR),
                new KeyframeIR<>(
                    1.0,
                    new Vec3d(to, 0, 0),
                    new Vec3d(to, 0, 0),
                    InterpolationIR.LINEAR)));
    return new BoneTrackIR(
        bone, position, null, null, TransformMode.ADDITIVE, TransformSpace.LOCAL);
  }

  private static SampledBoneTransformIR sampledBone(
      SampledClipIR clip, int frameIndex, BoneId bone) {
    return clip.frames().get(frameIndex).bones().stream()
        .filter(sampled -> sampled.bone().equals(bone))
        .findFirst()
        .orElseThrow();
  }

  private static void assertDiagnostic(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result, String code) {
    assertFalse(result.success());
    assertEquals(code, result.diagnostics().errors().get(0).code().value());
  }
}
