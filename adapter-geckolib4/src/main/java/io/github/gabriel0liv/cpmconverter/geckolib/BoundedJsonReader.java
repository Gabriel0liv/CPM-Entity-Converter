package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.*;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class BoundedJsonReader {
  Result<JsonNode> read(byte[] bytes, SourcePath source, int maxDepth) {
    try {
      JsonFactory factory =
          JsonFactory.builder()
              .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
              .streamReadConstraints(
                  StreamReadConstraints.builder().maxNestingDepth(maxDepth).build())
              .build();
      JsonNode node = new ObjectMapper(factory).readTree(bytes);
      return Result.success(node);
    } catch (StreamConstraintsException e) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_LIMIT_EXCEEDED),
              new SourceLocation(source, null, null, "/", null),
              "JSON nesting limit exceeded",
              "Reduce nesting depth",
              null,
              null,
              new TreeMap<>(
                  Map.of(
                      "limitName",
                      "maxNestingDepth",
                      "limit",
                      Integer.toString(maxDepth),
                      "observed",
                      ">" + maxDepth))));
    } catch (JsonParseException e) {
      String code =
          e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("duplicate")
              ? DiagnosticCodes.INPUT_PARSE_ERROR
              : DiagnosticCodes.INPUT_PARSE_ERROR;
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(code),
              SourceLocation.of(source),
              e.getMessage(),
              "Fix the JSON structure",
              null,
              null,
              new TreeMap<>()));
    } catch (java.io.IOException e) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_PARSE_ERROR),
              SourceLocation.of(source),
              "Invalid JSON",
              "Fix the JSON structure",
              null,
              null,
              new TreeMap<>()));
    }
  }
}
