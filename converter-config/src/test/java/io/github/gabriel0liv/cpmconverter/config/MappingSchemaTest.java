package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MappingSchemaTest {
  private final MappingLoader loader = new MappingLoader();

  @Test
  void rejectsUnknownNestedLookProperty() throws Exception {
    var result = load("{\"schemaVersion\":1,\"look\":{\"head\":\"head\",\"mystery\":true}}");

    assertFalse(result.success());
    assertHasSchemaError(result);
  }

  @Test
  void requiresClipInsideStateMapping() throws Exception {
    var result =
        load("{\"schemaVersion\":1,\"stateMappings\":{\"walking\":{\"mode\":\"absolute\"}}}");

    assertFalse(result.success());
    assertHasSchemaError(result);
  }

  @Test
  void enforcesRequestedFpsBoundaries() throws Exception {
    assertFalse(load("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":0}}").success());
    assertTrue(load("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":1}}").success());
    assertTrue(load("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":240}}").success());
    assertFalse(load("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":241}}").success());
  }

  @Test
  void rejectsUnknownRootStrategyAndEmptyNames() throws Exception {
    assertFalse(load("{\"schemaVersion\":1,\"rootStrategy\":\"magic\"}").success());
    assertFalse(load("{\"schemaVersion\":1,\"bones\":{\"head\":\"\"}}").success());
    assertFalse(
        load("{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{\"clip\":\"\"}}}").success());
  }

  @Test
  void rejectsUnsupportedSchemaVersionAndNegativeLookLimit() throws Exception {
    assertFalse(load("{\"schemaVersion\":2}").success());
    assertFalse(load("{\"schemaVersion\":1,\"look\":{\"limits\":{\"yaw\":-1}}}").success());
    assertTrue(
        load("{\"schemaVersion\":1,\"look\":{\"limits\":{\"yaw\":0,\"pitch\":45}}}").success());
  }

  private io.github.gabriel0liv.cpmconverter.diagnostics.Result<MappingDocumentV1> load(String json)
      throws Exception {
    Path path = Files.createTempFile("mapping-schema", ".json");
    try {
      Files.writeString(path, json);
      return loader.load(path);
    } finally {
      Files.deleteIfExists(path);
    }
  }

  private static void assertHasSchemaError(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<MappingDocumentV1> result) {
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.CONFIG_SCHEMA_INVALID)));
  }
}
