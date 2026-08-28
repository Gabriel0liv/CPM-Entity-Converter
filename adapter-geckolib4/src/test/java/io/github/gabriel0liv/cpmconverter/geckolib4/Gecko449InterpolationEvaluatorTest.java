package io.github.gabriel0liv.cpmconverter.geckolib4;

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
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.sampling.AnimationSampler;
import io.github.gabriel0liv.cpmconverter.sampling.InterpolationEvaluator;
import io.github.gabriel0liv.cpmconverter.sampling.SamplingRequest;
import io.github.gabriel0liv.cpmconverter.sampling.TimelineKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class Gecko449InterpolationEvaluatorTest {
  private static final ClipId CLIP = new ClipId("test.custom_easing");
  private static final BoneId HEAD = new BoneId("head");

  @Test
  void delegatesLinearStepAndPinnedSineValues() {
    InterpolationEvaluator evaluator = new Gecko449InterpolationEvaluator();

    assertEquals(0.5, evaluator.evaluate(InterpolationIR.LINEAR, List.of(), 0.5), 1e-12);
    assertEquals(0.0, evaluator.evaluate(InterpolationIR.STEP, List.of(), 0.5), 1e-12);
    assertEquals(
        0.2928932188134524,
        evaluator.evaluate(InterpolationIR.EASE_IN_SINE, List.of(), 0.5),
        1e-12);
  }

  @Test
  void preservesPinnedEaseInQuintQuirk() {
    InterpolationEvaluator evaluator = new Gecko449InterpolationEvaluator();

    assertEquals(
        0.0625, evaluator.evaluate(InterpolationIR.EASE_IN_QUINT, List.of(), 0.5), 1e-12);
  }

  @Test
  void forwardsEasingArgumentsWithoutReplacingThem() {
    InterpolationEvaluator evaluator = new Gecko449InterpolationEvaluator();

    assertEquals(0.5, evaluator.evaluate(InterpolationIR.STEP, List.of(4.0), 0.6), 1e-12);
  }

  @Test
  void directCustomEasingIsRejectedInsteadOfFallingBackToLinear() {
    InterpolationEvaluator evaluator = new Gecko449InterpolationEvaluator();

    assertThrows(
        IllegalArgumentException.class,
        () -> evaluator.evaluate(InterpolationIR.CUSTOM, List.of(), 0.5));
  }

  @Test
  void customEasingThroughAnimationSamplerReturnsStableDiagnostic() {
    var channel =
        new ChannelIR<>(
            "position",
            TransformMode.ADDITIVE,
            TransformSpace.LOCAL,
            List.of(
                new KeyframeIR<>(0.0, Vec3d.ZERO, Vec3d.ZERO, InterpolationIR.CUSTOM),
                new KeyframeIR<>(
                    1.0,
                    new Vec3d(10, 0, 0),
                    new Vec3d(10, 0, 0),
                    InterpolationIR.LINEAR)));
    var track =
        new BoneTrackIR(
            HEAD, channel, null, null, TransformMode.ADDITIVE, TransformSpace.LOCAL);
    var clip = new AnimationClipIR(CLIP, 1.0, PlaybackMode.LOOP, null, List.of(track));

    var result =
        new AnimationSampler()
            .sample(
                clip,
                List.of(HEAD),
                new SamplingRequest(CLIP, 2, TimelineKind.LOOP),
                new Gecko449InterpolationEvaluator());

    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.ANIM_CUSTOM_EASING_UNSUPPORTED,
        result.diagnostics().errors().get(0).code().value());
  }
}
