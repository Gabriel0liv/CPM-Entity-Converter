package io.github.gabriel0liv.cpmconverter.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.Set;

/** NON_PRODUCTION schema execution boundary for mapping-v1. */
final class MappingSchemaValidator {
  private static final Set<String> PROPERTIES =
      Set.of(
          "schemaVersion",
          "modelScale",
          "verticalOffset",
          "skin",
          "rootStrategy",
          "rootRoles",
          "bones",
          "clips",
          "look",
          "stateMappings",
          "sampling",
          "ignore",
          "diagnosticPolicy");

  DiagnosticBag validate(JsonNode root) {
    DiagnosticBag diagnostics = new DiagnosticBag();
    if (root == null || !root.isObject()) {
      return diagnostics.add(error("mapping document must be an object"));
    }
    if (!root.has("schemaVersion")) {
      diagnostics = diagnostics.add(error("schemaVersion is required"));
    } else if (!root.get("schemaVersion").canConvertToInt()
        || root.get("schemaVersion").asInt() != 1) {
      diagnostics = diagnostics.add(error("schemaVersion must be 1"));
    }
    root.fieldNames()
        .forEachRemaining(
            name -> {
              if (!PROPERTIES.contains(name)) {
                throw new UnknownPropertyException(name);
              }
            });
    return diagnostics;
  }

  private Diagnostic error(String message) {
    return Diagnostic.of(Severity.ERROR, DiagnosticCodes.CONFIG_SCHEMA_VERSION, message);
  }

  static final class UnknownPropertyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    UnknownPropertyException(String property) {
      super("unknown mapping property: " + property);
    }
  }
}
