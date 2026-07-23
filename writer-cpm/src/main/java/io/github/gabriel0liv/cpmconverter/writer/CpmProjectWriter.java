package io.github.gabriel0liv.cpmconverter.writer;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.projection.*;
import java.util.*;

public final class CpmProjectWriter {
  public Result<CpmProjectArtifact> write(CpmProjectWriteRequest request) {
    if (request == null || request.projection() == null || request.skinPng() == null || request.skinPng().length == 0)
      return Result.failure(error("request", "request and non-empty skin PNG are required"));
    var identified = request.projection();
    var validation = new CpmLogicalProjectionValidator().validate(identified.logicalProjection());
    validation = validation.addAll(new CpmStoreIdAssignmentValidator().validate(identified));
    if (validation.hasErrors()) return Result.failure(validation);
    try {
      var config = new CpmConfigJsonWriter().write(identified);
      var bytes = new CpmDeterministicZipWriter().write(config, request.skinPng());
      return Result.success(CpmProjectArtifact.of(bytes));
    } catch (java.io.IOException e) {
      return Result.failure(error("zip", e.getMessage() == null ? "write failed" : e.getMessage()));
    }
  }
  private static Diagnostic error(String field, String message) {
    return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_WRITE_FAILED), null, message, "Repair the CPM projection or texture payload before writing.", null, null, new TreeMap<>(Map.of("field", field)));
  }
}
