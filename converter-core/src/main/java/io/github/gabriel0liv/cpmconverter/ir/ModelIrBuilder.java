package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.List;

/** Result-based construction boundary for a structurally decoded model. */
public final class ModelIrBuilder {
  public Result<ModelIR> build(
      SourceDescriptor source,
      String geometryId,
      List<BoneIR> bones,
      List<BoneId> roots,
      List<AnimationClipIR> clips,
      List<TextureIR> textures,
      SourceLocation location) {
    try {
      return Result.success(
          new ModelIR(
              source, new GeometryId(geometryId), bones, roots, clips, textures, List.of()));
    } catch (RuntimeException exception) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              new DiagnosticCode(DiagnosticCodes.IR_INVALID_VALUE),
              location,
              exception.getMessage(),
              "Fix the decoded model fields",
              null,
              null,
              new java.util.TreeMap<>()));
    }
  }
}
