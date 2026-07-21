package io.github.gabriel0liv.cpmconverter.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads and executes the mapping-v1 structural schema before DTO decoding. */
public final class MappingLoader {
  private final ObjectMapper json =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
  private final ObjectMapper yaml =
      new ObjectMapper(new YAMLFactory())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
  private final JsonSchema schema;

  public MappingLoader() {
    try (var stream = MappingLoader.class.getResourceAsStream("/schema/mapping-v1.schema.json")) {
      if (stream == null) throw new IllegalStateException("mapping schema resource missing");
      schema =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
              .getSchema(json.readTree(stream));
    } catch (Exception exception) {
      throw new IllegalStateException("cannot load mapping schema", exception);
    }
  }

  public Result<MappingDocumentV1> load(Path path) {
    try {
      ObjectMapper mapper =
          path.toString().endsWith(".yaml") || path.toString().endsWith(".yml") ? yaml : json;
      JsonNode tree = mapper.readTree(Files.readString(path));
      var schemaErrors = schema.validate(tree);
      if (!schemaErrors.isEmpty()) {
        DiagnosticBag errors = new DiagnosticBag();
        for (var schemaError : schemaErrors) {
          errors =
              errors.add(
                  new Diagnostic(
                      Severity.ERROR,
                      new io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode(
                          DiagnosticCodes.CONFIG_SCHEMA_INVALID),
                      sourceLocation(path, null),
                      schemaError.getMessage(),
                      "correct the mapping field",
                      null,
                      null,
                      new java.util.TreeMap<>()));
        }
        return Result.failure(errors);
      }
      DiagnosticBag schemaDiagnostics;
      try {
        schemaDiagnostics = new MappingSchemaValidator().validate(tree);
      } catch (MappingSchemaValidator.UnknownPropertyException exception) {
        return Result.failure(
            new Diagnostic(
                Severity.ERROR,
                new io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode(
                    DiagnosticCodes.CONFIG_UNKNOWN_PROPERTY),
                sourceLocation(path, null),
                exception.getMessage(),
                "remove the unknown property",
                null,
                null,
                new java.util.TreeMap<>()));
      }
      if (schemaDiagnostics.hasErrors()) return Result.failure(schemaDiagnostics);
      MappingDocumentV1 document = mapper.treeToValue(tree, MappingDocumentV1.class);
      return Result.success(document, schemaDiagnostics);
    } catch (Exception exception) {
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR,
              DiagnosticCodes.CONFIG_PARSE_ERROR,
              exception.getMessage() == null ? "parse error" : exception.getMessage()));
    }
  }

  private static SourceLocation sourceLocation(Path path, String pointer) {
    String source = path.getFileName() == null ? "mapping" : path.getFileName().toString();
    return new SourceLocation(new SourcePath(source), null, null, pointer, null);
  }
}
