package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MappingLoaderTest {
  @Test
  void jsonAndYamlProduceEquivalentDocuments() throws Exception {
    Path json = Files.createTempFile("mapping", ".json");
    Path yaml = Files.createTempFile("mapping", ".yaml");
    Files.writeString(
        json,
        "{\"schemaVersion\":1,\"bones\":{\"head\":\"head\"},\"sampling\":{\"requestedFps\":24}}");
    Files.writeString(
        yaml, "schemaVersion: 1\nbones:\n  head: head\nsampling:\n  requestedFps: 24\n");
    var loader = new MappingLoader();
    assertEquals(loader.load(json).value(), loader.load(yaml).value());
  }

  @Test
  void malformedInputReturnsDiagnostic() throws Exception {
    Path file = Files.createTempFile("mapping", ".json");
    Files.writeString(file, "not-json");
    var result = new MappingLoader().load(file);
    assertFalse(result.success());
    assertEquals("CONFIG_PARSE_ERROR", result.diagnostics().errors().get(0).code().value());
  }
}
