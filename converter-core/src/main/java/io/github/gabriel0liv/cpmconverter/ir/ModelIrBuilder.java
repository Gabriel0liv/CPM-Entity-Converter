package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
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
    if (source == null) return failure("source descriptor is required", location);
    if (geometryId == null || geometryId.isBlank())
      return failure("geometry id is required", location);
    if (bones == null || roots == null || clips == null)
      return failure("model collections are required", location);
    return Result.success(
        new ModelIR(source, new GeometryId(geometryId), bones, roots, clips, textures, List.of()));
  }

  private Result<ModelIR> failure(String message, SourceLocation location) {
    return Result.failure(
        new Diagnostic(
            Severity.ERROR,
            DiagnosticCode.fromCatalog(DiagnosticCodes.IR_INVALID_VALUE),
            location,
            message == null ? "invalid model" : message,
            "Fix the decoded model fields",
            null,
            null,
            new java.util.TreeMap<>()));
  }
}
