package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.List;

/** Result-based construction boundary for decoded animation metadata. */
public final class AnimationClipIrBuilder {
  public Result<AnimationClipIR> build(
      String id,
      double duration,
      PlaybackMode playback,
      String customLoop,
      SourceLocation location) {
    try {
      return Result.success(
          new AnimationClipIR(
              new ClipId(id), duration, playback, customLoop, List.of(), List.of()));
    } catch (RuntimeException exception) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              new DiagnosticCode(DiagnosticCodes.IR_INVALID_VALUE),
              location,
              exception.getMessage(),
              "Provide a positive duration and valid playback mode",
              null,
              id,
              new java.util.TreeMap<>(java.util.Map.of("duration", Double.toString(duration)))));
    }
  }
}
