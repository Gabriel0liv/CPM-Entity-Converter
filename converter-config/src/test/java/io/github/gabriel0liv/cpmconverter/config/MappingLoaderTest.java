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
  void completeJsonAndYamlDocumentsHaveParityIncludingUnicodeAndNestedFields() throws Exception {
    Path json = Files.createTempFile("mapping-complete", ".json");
    Path yaml = Files.createTempFile("mapping-complete", ".yaml");
    Files.writeString(
        json,
        "{\"schemaVersion\":1,\"modelScale\":1.25,\"verticalOffset\":-2,"
            + "\"skin\":\"skins/é.png\",\"rootStrategy\":\"root_partition\","
            + "\"rootRoles\":{\"body\":\"corpo\"},\"bones\":{\"head\":\"cabeça\"},"
            + "\"clips\":{\"idle\":\"parado\"},\"look\":{\"head\":\"cabeça\","
            + "\"neck\":\"pescoço\",\"composition\":\"inherited_split\","
            + "\"neckInfluence\":0.35,\"headInfluence\":0.65,\"allowOverrotation\":true,"
            + "\"limits\":{\"yaw\":60,\"pitch\":45}},\"stateMappings\":{"
            + "\"idle\":{\"clip\":\"parado\",\"mode\":\"absolute\","
            + "\"optional\":false,\"requestedFps\":30}},\"sampling\":{"
            + "\"requestedFps\":24},\"ignore\":[\"x\"],\"diagnosticPolicy\":{"
            + "\"warningsAsErrors\":false,\"ignoreUnsupported\":true}}}");
    Files.writeString(
        yaml,
        "schemaVersion: 1\nmodelScale: 1.25\nverticalOffset: -2\n"
            + "skin: skins/é.png\nrootStrategy: root_partition\n"
            + "rootRoles:\n  body: corpo\nbones:\n  head: cabeça\nclips:\n  idle: parado\n"
            + "look:\n  head: cabeça\n  neck: pescoço\n  composition: inherited_split\n"
            + "  neckInfluence: 0.35\n  headInfluence: 0.65\n  allowOverrotation: true\n"
            + "  limits:\n    yaw: 60\n    pitch: 45\nstateMappings:\n  idle:\n"
            + "    clip: parado\n    mode: absolute\n    optional: false\n    requestedFps: 30\n"
            + "sampling:\n  requestedFps: 24\nignore:\n  - x\ndiagnosticPolicy:\n"
            + "  warningsAsErrors: false\n  ignoreUnsupported: true\n");
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
