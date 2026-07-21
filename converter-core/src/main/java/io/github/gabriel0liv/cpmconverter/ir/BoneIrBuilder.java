package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import java.util.List;

/** Minimal Result-based bone boundary for future adapters. */
public final class BoneIrBuilder {
  public Result<BoneIR> build(String id, String name, SourceLocation location) {
    if (name == null || name.isBlank()) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              new DiagnosticCode(DiagnosticCodes.IR_INVALID_VALUE),
              location,
              "bone name must not be blank",
              "Provide a source bone name",
              null,
              null,
              new java.util.TreeMap<>(java.util.Map.of("boneId", String.valueOf(id)))));
    }
    var created = IrValueFactory.boneId(id, location);
    if (!created.success()) return Result.failure(created.diagnostics());
    return Result.success(
        new BoneIR(
            created.value(),
            name,
            null,
            List.of(),
            io.github.gabriel0liv.cpmconverter.math.Transform.identity(),
            List.of(),
            location == null ? "unknown" : location.source().value()),
        created.diagnostics());
  }
}
