package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Result-based construction boundary for decoded animation metadata. */
public final class AnimationClipIrBuilder {
  public Result<AnimationClipIR> build(
      String id,
      double duration,
      PlaybackMode playback,
      String customLoop,
      SourceLocation location) {
    if (id == null || id.isBlank()) return failure("clip id must not be blank", location, id);
    if (!Double.isFinite(duration) || duration <= 0) {
      return failure("duration must be positive and finite", location, id);
    }
    if (playback == null) return failure("playback mode is required", location, id);
    if (playback == PlaybackMode.CUSTOM && (customLoop == null || customLoop.isBlank())) {
      return failure("custom playback requires source id", location, id);
    }
    return Result.success(
        new AnimationClipIR(
            new ClipId(id), duration, playback, customLoop, List.of(), List.of(), location));
  }

  private Result<AnimationClipIR> failure(String message, SourceLocation location, String id) {
    return Result.failure(
        new Diagnostic(
            Severity.ERROR,
            DiagnosticCode.fromCatalog(DiagnosticCodes.IR_INVALID_VALUE),
            location,
            message,
            "Provide a valid clip id, duration and playback mode",
            null,
            id,
            new TreeMap<>(Map.of("clipId", String.valueOf(id)))));
  }
}
