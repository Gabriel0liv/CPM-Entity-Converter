package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.ChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.KeyframeIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;

public final class VectorChannelSampler {
  private VectorChannelSampler() {}

  public static Result<Vec3d> sample(
      ChannelIR<Vec3d> channel,
      double timeSeconds,
      Vec3d identity,
      InterpolationEvaluator evaluator) {
    if (!Double.isFinite(timeSeconds) || timeSeconds < 0 || identity == null || evaluator == null) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID, "invalid vector sampling request");
    }
    if (channel == null) return Result.success(identity);

    List<KeyframeIR<Vec3d>> keyframes = channel.keyframes();
    if (keyframes.isEmpty()) {
      return failure(DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID, "channel has no keyframes");
    }
    for (KeyframeIR<Vec3d> keyframe : keyframes) {
      if (keyframe.incomingValue() == null || keyframe.outgoingValue() == null) {
        return failure(
            DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID, "channel keyframe has no value");
      }
      if (timeSeconds == keyframe.time()) return Result.success(keyframe.incomingValue());
    }

    KeyframeIR<Vec3d> first = keyframes.get(0);
    if (timeSeconds < first.time()) return Result.success(first.incomingValue());

    KeyframeIR<Vec3d> last = keyframes.get(keyframes.size() - 1);
    if (timeSeconds > last.time()) return Result.success(last.incomingValue());

    for (int i = 0; i < keyframes.size() - 1; i++) {
      KeyframeIR<Vec3d> left = keyframes.get(i);
      KeyframeIR<Vec3d> right = keyframes.get(i + 1);
      if (timeSeconds > left.time() && timeSeconds < right.time()) {
        double duration = right.time() - left.time();
        if (!Double.isFinite(duration) || duration <= 0) {
          return failure(
              DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
              "channel segment duration is not positive");
        }
        double progress = (timeSeconds - left.time()) / duration;
        double eased = evaluator.evaluate(left.interpolation(), left.easingArgs(), progress);
        if (!Double.isFinite(eased)) {
          return failure(
              DiagnosticCodes.ANIM_SAMPLING_NON_FINITE, "interpolation produced non-finite value");
        }
        Vec3d delta = right.incomingValue().subtract(left.outgoingValue());
        return Result.success(left.outgoingValue().add(delta.multiply(eased)));
      }
    }

    return failure(
        DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID, "sample time did not resolve a segment");
  }

  private static Result<Vec3d> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }
}
