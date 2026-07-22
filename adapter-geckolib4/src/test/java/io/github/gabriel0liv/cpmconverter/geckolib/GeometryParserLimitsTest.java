package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeometryParserLimitsTest {
  static Stream<Arguments> limits() {
    return Stream.of(
        Arguments.of(
            "maxBytes", new GeometryParserLimits(1, 64, 64, 4096, 4096, 16384, 256, 4096), base()),
        Arguments.of(
            "maxNestingDepth",
            new GeometryParserLimits(8_000_000, 1, 64, 4096, 4096, 16384, 256, 4096),
            "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{}]}"),
        Arguments.of(
            "maxGeometries",
            new GeometryParserLimits(8_000_000, 64, 1, 4096, 4096, 16384, 256, 4096),
            multi()),
        Arguments.of(
            "maxBones",
            new GeometryParserLimits(8_000_000, 64, 64, 0 + 1, 4096, 16384, 256, 4096),
            twoBones()),
        Arguments.of(
            "maxCubesPerBone",
            new GeometryParserLimits(8_000_000, 64, 64, 4096, 1, 16384, 256, 4096),
            cubes()),
        Arguments.of(
            "maxTotalCubes",
            new GeometryParserLimits(8_000_000, 64, 64, 4096, 4096, 0 + 1, 256, 4096),
            cubes()),
        Arguments.of(
            "maxHierarchyDepth",
            new GeometryParserLimits(8_000_000, 64, 64, 4096, 4096, 16384, 1, 4096),
            deep()),
        Arguments.of(
            "maxStringLength",
            new GeometryParserLimits(8_000_000, 64, 64, 4096, 4096, 16384, 256, 3),
            longName()));
  }

  @ParameterizedTest
  @MethodSource("limits")
  void rejectsConfiguredLimit(String name, GeometryParserLimits limits, String json) {
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("fixture.geo.json"),
                new GeometryParseRequest(null, limits));
    assertFalse(result.success());
    assertNull(result.value());
    var diagnostic =
        result.diagnostics().errors().stream()
            .filter(d -> d.code().value().equals(DiagnosticCodes.INPUT_LIMIT_EXCEEDED))
            .findFirst()
            .orElseThrow(() -> new AssertionError(name));
    org.junit.jupiter.api.Assertions.assertEquals(Severity.ERROR, diagnostic.severity());
    org.junit.jupiter.api.Assertions.assertNotNull(diagnostic.location());
    org.junit.jupiter.api.Assertions.assertEquals(
        "fixture.geo.json", diagnostic.location().source().value());
    org.junit.jupiter.api.Assertions.assertEquals(name, diagnostic.context().get("limitName"));
    org.junit.jupiter.api.Assertions.assertNotNull(diagnostic.context().get("limit"));
    org.junit.jupiter.api.Assertions.assertNotNull(diagnostic.context().get("observed"));
    org.junit.jupiter.api.Assertions.assertFalse(diagnostic.suggestion().isBlank());
  }

  @ParameterizedTest
  @MethodSource("limits")
  void limitDiagnosticsHavePointers(String name, GeometryParserLimits limits, String json) {
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("fixture.geo.json"),
                new GeometryParseRequest(null, limits));
    var diagnostic =
        result.diagnostics().errors().stream()
            .filter(d -> name.equals(d.context().get("limitName")))
            .findFirst()
            .orElseThrow();
    org.junit.jupiter.api.Assertions.assertNotNull(diagnostic.location().jsonPointer());
  }

  @org.junit.jupiter.api.Test
  void maxStringLengthAppliesToBoneNames() {
    var result =
        new GeckoGeometryParser()
            .parse(
                longBoneName().getBytes(StandardCharsets.UTF_8),
                new SourcePath("fixture.geo.json"),
                new GeometryParseRequest(
                    null, new GeometryParserLimits(8_000_000, 64, 64, 4096, 4096, 16384, 256, 3)));
    assertFalse(result.success());
    org.junit.jupiter.api.Assertions.assertEquals(
        "maxStringLength", result.diagnostics().errors().get(0).context().get("limitName"));
  }

  private static String base() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"body\"},\"bones\":[{\"name\":\"body\"}]}]}";
  }

  private static String multi() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"one\"},\"bones\":[]},{\"description\":{\"identifier\":\"two\"},\"bones\":[]}] }";
  }

  private static String cubes() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\",\"cubes\":[{\"size\":[1,1,1]},{\"size\":[1,1,1]}]}]}]}";
  }

  private static String deep() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"root\"},{\"name\":\"child\",\"parent\":\"root\"}]}]}";
  }

  private static String twoBones() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"one\"},{\"name\":\"two\"}]}]}";
  }

  private static String longName() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"long\"},\"bones\":[{\"name\":\"body\"}]}]}";
  }

  private static String longBoneName() {
    return "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"long\"}]}]}";
  }
}
