package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeometryBoundaryValuesTest {
  static Stream<Arguments> invalidFields() {
    return Stream.of(
        Arguments.of("pivot2", "\"pivot\":[1,2]", "/minecraft:geometry/0/bones/0/pivot"),
        Arguments.of("pivot4", "\"pivot\":[1,2,3,4]", "/minecraft:geometry/0/bones/0/pivot"),
        Arguments.of(
            "pivotString", "\"pivot\":[1,\"x\",3]", "/minecraft:geometry/0/bones/0/pivot/1"),
        Arguments.of("pivotNull", "\"pivot\":[1,null,3]", "/minecraft:geometry/0/bones/0/pivot/1"),
        Arguments.of(
            "origin2",
            "\"cubes\":[{\"origin\":[1,2],\"size\":[1,1,1]}]",
            "/minecraft:geometry/0/bones/0/cubes/0/origin"),
        Arguments.of(
            "originString",
            "\"cubes\":[{\"origin\":\"bad\",\"size\":[1,1,1]}]",
            "/minecraft:geometry/0/bones/0/cubes/0/origin"),
        Arguments.of(
            "cubePivot",
            "\"cubes\":[{\"origin\":[0,0,0],\"size\":[1,1,1],\"pivot\":[1,2]}]",
            "/minecraft:geometry/0/bones/0/cubes/0/pivot"),
        Arguments.of(
            "cubeRotation",
            "\"cubes\":[{\"origin\":[0,0,0],\"size\":[1,1,1],\"rotation\":[1,2,3,4]}]",
            "/minecraft:geometry/0/bones/0/cubes/0/rotation"),
        Arguments.of(
            "sizeNegative",
            "\"cubes\":[{\"origin\":[0,0,0],\"size\":[-1,1,1]}]",
            "/minecraft:geometry/0/bones/0/cubes/0/size"),
        Arguments.of(
            "inflateString", "\"inflate\":\"x\"", "/minecraft:geometry/0/bones/0/inflate"));
  }

  @ParameterizedTest
  @MethodSource("invalidFields")
  void invalidPresentValuesFailWithoutDefaults(String name, String field, String pointer) {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\","
            + field
            + "}]}]}";
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("values.geo.json"),
                GeometryParseRequest.defaults());
    assertFalse(result.success(), name);
    var diagnostic =
        result.diagnostics().errors().stream()
            .filter(d -> pointer.equals(d.location().jsonPointer()))
            .findFirst()
            .orElseThrow();
    assertEquals(Severity.ERROR, diagnostic.severity());
    assertEquals(DiagnosticCodes.IR_INVALID_VALUE, diagnostic.code().value());
    assertFalse(diagnostic.suggestion().isBlank());
    assertNull(result.value());
  }

  @org.junit.jupiter.api.Test
  void unicodeIdentifierAndCaseSensitiveSelectionArePreserved() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"G-é\"},\"bones\":[{\"name\":\"body\"}]}]}";
    var ok =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("id.geo.json"),
                new GeometryParseRequest(
                    new io.github.gabriel0liv.cpmconverter.ir.GeometryId("G-é"),
                    GeometryParserLimits.defaults()));
    var wrong =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("id.geo.json"),
                new GeometryParseRequest(
                    new io.github.gabriel0liv.cpmconverter.ir.GeometryId("g-é"),
                    GeometryParserLimits.defaults()));
    assertTrue(ok.success());
    assertFalse(wrong.success());
  }
}
