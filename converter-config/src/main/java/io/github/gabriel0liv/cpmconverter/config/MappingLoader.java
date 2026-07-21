package io.github.gabriel0liv.cpmconverter.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads and executes the mapping-v1 structural schema before DTO decoding. */
public final class MappingLoader {
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public Result<MappingDocumentV1> load(Path path) {
        try {
            ObjectMapper mapper = path.toString().endsWith(".yaml") || path.toString().endsWith(".yml") ? yaml : json;
            JsonNode tree = mapper.readTree(Files.readString(path));
            DiagnosticBag schemaDiagnostics;
            try {
                schemaDiagnostics = new MappingSchemaValidator().validate(tree);
            } catch (MappingSchemaValidator.UnknownPropertyException exception) {
                return Result.failure(Diagnostic.of(Severity.ERROR, "CONFIG_UNKNOWN_PROPERTY", exception.getMessage()));
            }
            if (schemaDiagnostics.hasErrors()) return Result.failure(schemaDiagnostics);
            MappingDocumentV1 document = mapper.treeToValue(tree, MappingDocumentV1.class);
            return Result.success(document, schemaDiagnostics);
        } catch (Exception exception) {
            return Result.failure(Diagnostic.of(Severity.ERROR, "CONFIG_PARSE_ERROR",
                    exception.getMessage() == null ? "parse error" : exception.getMessage()));
        }
    }
}
