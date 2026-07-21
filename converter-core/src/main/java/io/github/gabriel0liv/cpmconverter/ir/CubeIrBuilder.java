package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;

/** Boundary reserved for complete cube decoding; no production parser is implemented. */
public final class CubeIrBuilder {
  public Result<CubeIR> unsupported() {
    return Result.failure(
        Diagnostic.of(
            Severity.ERROR,
            DiagnosticCodes.IR_INVALID_VALUE,
            "cube construction requires decoded geometry fields"));
  }
}
