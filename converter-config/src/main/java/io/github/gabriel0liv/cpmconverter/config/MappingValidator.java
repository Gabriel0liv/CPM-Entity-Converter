package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;

/** Performs semantic checks after JSON/YAML decoding. */
public final class MappingValidator {
  public DiagnosticBag validate(MappingDocumentV1 document) {
    DiagnosticBag diagnostics = new DiagnosticBag();
    if (document == null || document.schemaVersion() == null || document.schemaVersion() != 1) {
      diagnostics =
          diagnostics.add(error(DiagnosticCodes.CONFIG_SCHEMA_VERSION, "schemaVersion must be 1"));
      return diagnostics;
    }
    if (document.modelScale() != null
        && (!Double.isFinite(document.modelScale()) || document.modelScale() <= 0)) {
      diagnostics =
          diagnostics.add(
              error(DiagnosticCodes.CONFIG_NON_FINITE, "modelScale must be finite and positive"));
    }
    if (document.verticalOffset() != null && !Double.isFinite(document.verticalOffset())) {
      diagnostics =
          diagnostics.add(
              error(DiagnosticCodes.CONFIG_NON_FINITE, "verticalOffset must be finite"));
    }
    if (document.sampling() != null) {
      Integer fps = document.sampling().requestedFps();
      if (fps == null || fps < 1 || fps > 240) {
        diagnostics =
            diagnostics.add(
                error(DiagnosticCodes.CONFIG_SAMPLING_RANGE, "requestedFps must be 1..240"));
      }
    }
    if (document.look() != null) {
      MappingDocumentV1.Look look = document.look();
      if (look.neckInfluence() != null && !Double.isFinite(look.neckInfluence())
          || look.headInfluence() != null && !Double.isFinite(look.headInfluence())) {
        diagnostics =
            diagnostics.add(error(DiagnosticCodes.CONFIG_NON_FINITE, "influence must be finite"));
      }
      if (look.neckInfluence() != null && look.neckInfluence() < 0
          || look.headInfluence() != null && look.headInfluence() < 0) {
        diagnostics =
            diagnostics.add(
                error(DiagnosticCodes.CONFIG_INFLUENCE_RANGE, "influence must not be negative"));
      }
      if ("inherited_split".equals(look.composition())
          && look.neckInfluence() != null
          && look.headInfluence() != null
          && look.neckInfluence() + look.headInfluence() > 1.000001
          && !Boolean.TRUE.equals(look.allowOverrotation())) {
        diagnostics =
            diagnostics.add(
                error(DiagnosticCodes.CONFIG_OVERROTATION, "overrotation requires permission"));
      }
    }
    for (MappingDocumentV1.StateMapping mapping : document.stateMappings().values()) {
      if (mapping.requestedFps() != null
          && (mapping.requestedFps() < 1 || mapping.requestedFps() > 240)) {
        diagnostics =
            diagnostics.add(
                error(DiagnosticCodes.CONFIG_SAMPLING_RANGE, "state requestedFps must be 1..240"));
      }
    }
    return diagnostics;
  }

  private Diagnostic error(String code, String message) {
    return Diagnostic.of(
        Severity.ERROR,
        io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode.fromCatalog(code),
        message);
  }
}
