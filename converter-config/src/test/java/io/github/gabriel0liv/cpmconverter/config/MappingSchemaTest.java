package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MappingSchemaTest {
  @Test
  void schemaRejectsUnknownNestedProperty() throws Exception {
    Path file = Files.createTempFile("mapping-schema", ".json");
    Files.writeString(file, "{\"schemaVersion\":1,\"look\":{\"unknown\":true}}");
    var result = new MappingLoader().load(file);
    assertFalse(result.success());
    assertTrue(result.diagnostics().errors().stream().anyMatch(d -> d.location() != null));
  }

  @Test
  void schemaEnforcesEnumsAndBounds() throws Exception {
    Path file = Files.createTempFile("mapping-schema", ".json");
    Files.writeString(
        file,
        "{\"schemaVersion\":1,\"rootStrategy\":\"invalid\",\"sampling\":{\"requestedFps\":241}}");
    var result = new MappingLoader().load(file);
    assertFalse(result.success());
    assertTrue(result.diagnostics().errors().size() >= 2);
  }

  @Test
  void schemaRequiresVersion() throws Exception {
    Path file = Files.createTempFile("mapping-schema", ".json");
    Files.writeString(file, "{\"bones\":{}}");
    assertFalse(new MappingLoader().load(file).success());
  }
}
