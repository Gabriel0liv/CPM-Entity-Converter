package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.RotationContinuityIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationChannelIR;
import io.github.gabriel0liv.cpmconverter.ir.SourceRotationKeyframeIR;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.math.Vec3i;
import java.util.List;
import java.util.Optional;

public final class RotationChannelSampler {
  private RotationChannelSampler() {}

  public record RotationSample(Quatd rotation, RotationContinuityIR continuity) {
    public RotationSample {
      if (rotation == null || continuity == null)
        throw new IllegalArgumentException("rotation sample");
    }
  }

  public static Result<RotationSample> sample(
      SourceRotationChannelIR channel, double timeSeconds, InterpolationEvaluator evaluator) {
    if (!Double.isFinite(timeSeconds) || timeSeconds < 0 || evaluator == null) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_REQUEST_INVALID, "invalid rotation sampling request");
    }
    if (channel == null) return Result.success(sampled(Vec3d.ZERO));

    List<SourceRotationKeyframeIR> keyframes = channel.keyframes();
    if (keyframes.isEmpty()) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID, "rotation channel has no keyframes");
    }

    for (SourceRotationKeyframeIR keyframe : keyframes) {
      if (keyframe.incomingValue() == null || keyframe.outgoingValue() == null) {
        return failure(
            DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
            "rotation channel keyframe has no value");
      }
      if (timeSeconds == keyframe.timeSeconds()) return sampledResult(keyframe.incomingValue());
    }

    SourceRotationKeyframeIR first = keyframes.get(0);
    if (timeSeconds < first.timeSeconds()) return sampledResult(first.incomingValue());

    SourceRotationKeyframeIR last = keyframes.get(keyframes.size() - 1);
    if (timeSeconds > last.timeSeconds()) return sampledResult(last.incomingValue());

    for (int i = 0; i < keyframes.size() - 1; i++) {
      SourceRotationKeyframeIR left = keyframes.get(i);
      SourceRotationKeyframeIR right = keyframes.get(i + 1);
      if (timeSeconds > left.timeSeconds() && timeSeconds < right.timeSeconds()) {
        double duration = right.timeSeconds() - left.timeSeconds();
        if (!Double.isFinite(duration) || duration <= 0) {
          return failure(
              DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
              "rotation channel segment duration is not positive");
        }

        double progress = (timeSeconds - left.timeSeconds()) / duration;
        double eased =
            evaluator.evaluate(left.interpolationAfter(), left.easingArgsAfter(), progress);
        if (!Double.isFinite(eased)) {
          return failure(
              DiagnosticCodes.ANIM_SAMPLING_NON_FINITE,
              "rotation interpolation produced non-finite value");
        }

        Vec3d delta = right.incomingValue().subtract(left.outgoingValue());
        return sampledResult(left.outgoingValue().add(delta.multiply(eased)));
      }
    }

    return failure(
        DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
        "rotation sample time did not resolve a segment");
  }

  private static Result<RotationSample> sampledResult(Vec3d sampledEuler) {
    try {
      return Result.success(sampled(sampledEuler));
    } catch (IllegalArgumentException exception) {
      return failure(
          DiagnosticCodes.ANIM_SAMPLING_CHANNEL_INVALID,
          "rotation sample cannot preserve authored winding");
    }
  }

  private static RotationSample sampled(Vec3d sampledEuler) {
    Vec3i winding =
        new Vec3i(winding(sampledEuler.x()), winding(sampledEuler.y()), winding(sampledEuler.z()));
    RotationContinuityIR continuity =
        new RotationContinuityIR(sampledEuler, winding, Optional.empty());
    Quatd rotation =
        Quatd.fromEulerZYX(
            Math.toRadians(sampledEuler.x()),
            Math.toRadians(sampledEuler.y()),
            Math.toRadians(sampledEuler.z()));
    return new RotationSample(rotation, continuity);
  }

  private static int winding(double degrees) {
    double principal = degrees % 360.0;
    if (principal <= -180.0) principal += 360.0;
    if (principal > 180.0) principal -= 360.0;

    double turns = Math.rint((degrees - principal) / 360.0);
    if (!Double.isFinite(turns) || turns < Integer.MIN_VALUE || turns > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("rotation winding");
    }
    return (int) turns;
  }

  private static Result<RotationSample> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }
}
