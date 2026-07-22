package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Table-driven coverage of the executable mapping-v1 JSON Schema. */
class MappingSchemaMatrixTest {
  private final MappingLoader loader = new MappingLoader();

  static Stream<Arguments> invalidDocuments() {
    return Stream.of(
        Arguments.of("root array", "[]"),
        Arguments.of("root string", "\"mapping\""),
        Arguments.of("root null", "null"),
        Arguments.of("missing version", "{}"),
        Arguments.of("wrong version", "{\"schemaVersion\":2}"),
        Arguments.of("version type", "{\"schemaVersion\":\"1\"}"),
        Arguments.of("unknown top-level", "{\"schemaVersion\":1,\"extra\":true}"),
        Arguments.of("scale type", "{\"schemaVersion\":1,\"modelScale\":\"1\"}"),
        Arguments.of("scale boundary", "{\"schemaVersion\":1,\"modelScale\":0}"),
        Arguments.of("scale NaN", "{\"schemaVersion\":1,\"modelScale\":NaN}"),
        Arguments.of("scale infinity", "{\"schemaVersion\":1,\"modelScale\":Infinity}"),
        Arguments.of("offset type", "{\"schemaVersion\":1,\"verticalOffset\":false}"),
        Arguments.of("skin empty", "{\"schemaVersion\":1,\"skin\":\"\"}"),
        Arguments.of("root strategy enum", "{\"schemaVersion\":1,\"rootStrategy\":\"other\"}"),
        Arguments.of("bones type", "{\"schemaVersion\":1,\"bones\":[]}"),
        Arguments.of("bones value", "{\"schemaVersion\":1,\"bones\":{\"head\":3}}"),
        Arguments.of("bones nested value", "{\"schemaVersion\":1,\"bones\":{\"head\":{}}}"),
        Arguments.of("root roles null", "{\"schemaVersion\":1,\"rootRoles\":null}"),
        Arguments.of("clips null", "{\"schemaVersion\":1,\"clips\":null}"),
        Arguments.of("look type", "{\"schemaVersion\":1,\"look\":\"head\"}"),
        Arguments.of("look unknown", "{\"schemaVersion\":1,\"look\":{\"extra\":1}}"),
        Arguments.of(
            "look composition", "{\"schemaVersion\":1,\"look\":{\"composition\":\"bad\"}}"),
        Arguments.of(
            "look influence type", "{\"schemaVersion\":1,\"look\":{\"headInfluence\":\"1\"}}"),
        Arguments.of("state type", "{\"schemaVersion\":1,\"stateMappings\":[]}"),
        Arguments.of(
            "state required clip", "{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{}}}"),
        Arguments.of(
            "state unknown",
            "{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{\"clip\":\"x\",\"extra\":1}}}"),
        Arguments.of(
            "state fps low",
            "{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{\"clip\":\"x\",\"requestedFps\":0}}}"),
        Arguments.of(
            "state fps high",
            "{\"schemaVersion\":1,\"stateMappings\":{\"idle\":{\"clip\":\"x\",\"requestedFps\":241}}}"),
        Arguments.of("sampling type", "{\"schemaVersion\":1,\"sampling\":[]}"),
        Arguments.of(
            "sampling fps type", "{\"schemaVersion\":1,\"sampling\":{\"requestedFps\":\"24\"}}"),
        Arguments.of("ignore type", "{\"schemaVersion\":1,\"ignore\":{}}"),
        Arguments.of(
            "policy unknown", "{\"schemaVersion\":1,\"diagnosticPolicy\":{\"extra\":true}}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidDocuments")
  void rejectsInvalidDocuments(String name, String content) throws Exception {
    assertFalse(load(content).success(), name);
  }

  @Test
  void acceptsCompleteUnicodeDocument() throws Exception {
    String content =
        "{\"schemaVersion\":1,\"modelScale\":1.5,\"verticalOffset\":-2,"
            + "\"skin\":\"skins/é.png\",\"rootStrategy\":\"root_partition\","
            + "\"rootRoles\":{\"corpo\":\"body\"},\"bones\":{\"cabeça\":\"head\"},"
            + "\"clips\":{\"parado\":\"idle\"},\"look\":{\"head\":\"head\","
            + "\"neck\":\"neck\",\"composition\":\"inherited_split\","
            + "\"neckInfluence\":0.35,\"headInfluence\":0.65,\"allowOverrotation\":true,"
            + "\"limits\":{\"yaw\":60,\"pitch\":45}},\"stateMappings\":{"
            + "\"idle\":{\"clip\":\"idle\",\"mode\":\"absolute\",\"optional\":false,"
            + "\"requestedFps\":30}},\"sampling\":{\"requestedFps\":24},"
            + "\"ignore\":[\"unsupported.foo\"],\"diagnosticPolicy\":{"
            + "\"warningsAsErrors\":false,\"ignoreUnsupported\":false}}";
    assertTrue(load(content).success());
  }

  private ResultLike load(String content) throws Exception {
    Path file = Files.createTempFile("mapping-matrix", ".json");
    Files.writeString(file, content);
    var result = loader.load(file);
    return new ResultLike(result.success());
  }

  private record ResultLike(boolean success) {}
}
