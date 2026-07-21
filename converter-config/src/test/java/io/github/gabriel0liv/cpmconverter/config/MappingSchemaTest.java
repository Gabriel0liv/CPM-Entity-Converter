package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Executable coverage for the classpath draft 2020-12 mapping schema. */
class MappingSchemaTest {
  private final MappingLoader loader = new MappingLoader();

  @Test
  void rejectsUnknownTopLevelAndNestedProperties() throws Exception {
    assertError("{\"schemaVersion\":1,\"unknown\":true}");
    assertError("{\"schemaVersion\":1,\"look\":{\"unknown\":true}}");
    assertError(
        "{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{\"clip\":\"x\",\"unknown\":1}}}");
  }

  @Test
  void enforcesRequiredVersionAndObjectRoot() throws Exception {
    assertError("{}");
    assertError("[]");
    assertError("{\"schemaVersion\":2}");
    assertError("{\"schemaVersion\":\"1\"}");
  }

  @Test
  void enforcesNestedTypesEnumsAndRanges() throws Exception {
    assertError("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":0}}");
    assertError("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":241}}");
    assertError("{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":\"24\"}}");
    assertError("{\"schemaVersion\":1,\"rootStrategy\":\"other\"}");
    assertError("{\"schemaVersion\":1,\"look\":{\"composition\":\"other\"}}");
    assertError("{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{}}}");
    assertError("{\"schemaVersion\":1,\"stateMappings\":[]}");
  }

  @Test
  void acceptsCompleteDocument() throws Exception {
    String json =
        "{\"schemaVersion\":1,\"modelScale\":1.25,\"verticalOffset\":0.5,"
            + "\"skin\":\"skins/é.png\",\"rootStrategy\":\"single_anchor\","
            + "\"rootRoles\":{\"body\":\"body\"},\"bones\":{\"head\":\"head\"},"
            + "\"clips\":{\"idle\":\"idle\"},\"look\":{\"head\":\"head\","
            + "\"neck\":\"neck\",\"composition\":\"inherited_split\","
            + "\"neckInfluence\":0.35,\"headInfluence\":0.65,\"allowOverrotation\":false,"
            + "\"limits\":{\"yaw\":60}},\"stateMappings\":{\"idle\":{"
            + "\"clip\":\"idle\",\"mode\":\"absolute\",\"optional\":false,"
            + "\"requestedFps\":30}},\"sampling\":{\"requestedFps\":24},"
            + "\"ignore\":[\"unsupported.foo\"],\"diagnosticPolicy\":{"
            + "\"warningsAsErrors\":false,\"ignoreUnsupported\":false}}";
    var result = load(json);
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals("é.png", result.value().skin().substring(result.value().skin().length() - 5));
  }

  private void assertError(String json) throws Exception {
    assertFalse(load(json).success(), json);
  }

  private Result<MappingDocumentV1> load(String json) throws Exception {
    Path path = Files.createTempFile("mapping-schema", ".json");
    Files.writeString(path, json);
    var result = loader.load(path);
    return result;
  }
}
