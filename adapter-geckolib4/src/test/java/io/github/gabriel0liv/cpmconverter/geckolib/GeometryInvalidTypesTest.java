package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeometryInvalidTypesTest {
  static Stream<Arguments> invalidTypes() {
    return Stream.of(
        Arguments.of("bone mirror", "boneMirror", "/minecraft:geometry/0/bones/0/mirror"),
        Arguments.of("cube mirror", "cubeMirror", "/minecraft:geometry/0/bones/0/cubes/0/mirror"),
        Arguments.of(
            "texture width", "texture_width", "/minecraft:geometry/0/description/texture_width"),
        Arguments.of(
            "texture height",
            "texture_height",
            "/minecraft:geometry/0/description/texture_height"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTypes")
  void presentInvalidScalarDoesNotBecomeDefault(String name, String field, String pointer) {
    String json =
        switch (field) {
          case "boneMirror" -> base("\"mirror\":\"yes\"");
          case "cubeMirror" ->
              base("\"cubes\":[{\"origin\":[0,0,0],\"size\":[1,1,1],\"mirror\":1}]");
          case "texture_width" -> baseDescription("\"texture_width\":\"32\"");
          default -> baseDescription("\"texture_height\":1.5");
        };
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("fixture.geo.json"),
                GeometryParseRequest.defaults());
    assertFalse(result.success(), name);
    assertNotNull(
        result.diagnostics().errors().stream()
            .filter(d -> pointer.equals(d.location().jsonPointer()))
            .findFirst()
            .orElse(null));
  }

  private static String base(String fields) {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\","
        + fields
        + "}]}]}";
  }

  private static String baseDescription(String fields) {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\","
        + fields
        + "},\"bones\":[{\"name\":\"body\"}]}]}";
  }
}
