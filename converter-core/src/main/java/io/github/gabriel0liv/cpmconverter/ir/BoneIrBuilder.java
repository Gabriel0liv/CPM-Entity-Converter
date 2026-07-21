package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.List;

/** Minimal Result-based bone boundary for future adapters. */
public final class BoneIrBuilder {
  public Result<BoneIR> build(String id, String name, SourceLocation location) {
    var created = IrValueFactory.boneId(id, location);
    if (!created.success()) return Result.failure(created.diagnostics());
    try {
      return Result.success(
          new BoneIR(
              created.value(),
              name,
              null,
              List.of(),
              io.github.gabriel0liv.cpmconverter.math.Transform.identity(),
              List.of(),
              location.source().value()),
          created.diagnostics());
    } catch (RuntimeException exception) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              new DiagnosticCode(DiagnosticCodes.IR_INVALID_VALUE),
              location,
              exception.getMessage(),
              null,
              name,
              null,
              new java.util.TreeMap<>()));
    }
  }
}
