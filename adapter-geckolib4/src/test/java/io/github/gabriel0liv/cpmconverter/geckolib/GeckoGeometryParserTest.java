package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GeckoGeometryParserTest {
  private final GeckoGeometryParser parser = new GeckoGeometryParser();

  @Test
  void acceptsOnlyGeometryVersion1120() {
    assertTrue(parse(json("1.12.0", "body", "")).success());
    assertCode(json("1.14.0", "body", ""), DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);
    assertCode("{\"minecraft:geometry\":[]}", DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);
    assertCode(
        "{\"format_version\":1.12,\"minecraft:geometry\":[]}",
        DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);
  }

  @Test
  void selectsByExactIdentifierAndPreservesOrder() {
    String source =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":["
            + geometry("one", "body")
            + ","
            + geometry("two", "head")
            + "]}";
    var result =
        parser.parse(
            source.getBytes(StandardCharsets.UTF_8),
            new SourcePath("fixture.geo.json"),
            new GeometryParseRequest(new GeometryId("two"), GeometryParserLimits.defaults()));
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals("head", result.value().bones().get(0).sourceName());
    assertFalse(
        parser
            .parse(
                source.getBytes(StandardCharsets.UTF_8),
                new SourcePath("fixture.geo.json"),
                GeometryParseRequest.defaults())
            .success());
    assertCode(source, DiagnosticCodes.GEO_MULTIPLE_MODELS);
  }

  @Test
  void rejectsAmbiguousExactIdentifier() {
    String source =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":["
            + geometry("same", "one")
            + ","
            + geometry("same", "two")
            + "]}";
    var result =
        parser.parse(
            source.getBytes(StandardCharsets.UTF_8),
            new SourcePath("fixture.geo.json"),
            new GeometryParseRequest(new GeometryId("same"), GeometryParserLimits.defaults()));
    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.GEO_MODEL_AMBIGUOUS, result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void computesLocalBindTranslationAndDeterministicIds() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":["
            + "{\"name\":\"body\",\"pivot\":[0,0,0],\"cubes\":[{\"origin\":[0,0,0],\"size\":[2,3,4],\"uv\":[0,0]}]},"
            + "{\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,8,0],\"rotation\":[0,0,90]}]}]}";
    var result = parse(json);
    assertTrue(result.success(), result.diagnostics().all().toString());
    var body = result.value().bones().get(0);
    var head = result.value().bones().get(1);
    assertEquals("g/bone/0", body.id().value());
    assertEquals("g/bone/0", head.parent().value());
    assertEquals(-8, head.bindLocal().translation().y(), 1e-9);
    assertEquals("g/bone/0/cube/0", body.cubes().get(0).id().value());
    assertEquals(
        "/minecraft:geometry/0/bones/0/cubes/0", body.cubes().get(0).source().jsonPointer());
  }

  @Test
  void preservesCubeBoundaryAndInflateInheritance() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\",\"inflate\":0.5,\"mirror\":true,\"cubes\":[{\"origin\":[1,2,3],\"size\":[2,2,2],\"pivot\":[4,5,6],\"rotation\":[12,0,27],\"uv\":{\"north\":{\"uv\":[0,0],\"uv_size\":[2,2]}}}]}]}]}";
    var cube = parse(json).value().bones().get(0).cubes().get(0);
    assertEquals(0.5, cube.inflate(), 1e-9);
    assertFalse(cube.mirror());
    assertEquals(0.5, parse(json).value().bones().get(0).inflate(), 1e-9);
    assertTrue(parse(json).value().bones().get(0).mirror());
    assertEquals("{\"north\":{\"uv\":[0,0],\"uv_size\":[2,2]}}", cube.rawUv().canonicalJson());
  }

  @Test
  void rejectsGraphErrorsAndInvalidVectors() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"a\",\"parent\":\"missing\",\"pivot\":[1,2]}]}]}";
    var result = parse(json);
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.GEO_PARENT_NOT_FOUND)));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals("IR_INVALID_VALUE")));
  }

  @Test
  void enforcesLimits() {
    GeometryParserLimits limits =
        new GeometryParserLimits(10, 64, 64, 4096, 4096, 16384, 256, 4096);
    var result =
        parser.parse(
            json("1.12.0", "body", "").getBytes(StandardCharsets.UTF_8),
            new SourcePath("fixture.geo.json"),
            new GeometryParseRequest(null, limits));
    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.INPUT_LIMIT_EXCEEDED, result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void surfacesUnsupportedFeatures() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\",\"poly_mesh\":{}}]}]}";
    var result = parse(json);
    assertFalse(result.success());
    assertEquals(
        DiagnosticCodes.GEO_MESH_UNSUPPORTED, result.diagnostics().errors().get(0).code().value());
    assertEquals("poly_mesh", result.diagnostics().errors().get(0).context().get("feature"));
  }

  @Test
  void validatesParsedGraphBoundary() {
    var parsed = parse(json("1.12.0", "body", "")).value();
    var result = new ParsedGeometryValidator().validate(parsed);
    assertTrue(result.success(), result.diagnostics().all().toString());
  }

  private io.github.gabriel0liv.cpmconverter.diagnostics.Result<ParsedGeometry> parse(String json) {
    var result =
        parser.parse(
            json.getBytes(StandardCharsets.UTF_8),
            new SourcePath("fixture.geo.json"),
            GeometryParseRequest.defaults());
    assertNotNull(result);
    return result;
  }

  private void assertCode(String json, String code) {
    var result =
        parser.parse(
            json.getBytes(StandardCharsets.UTF_8),
            new SourcePath("fixture.geo.json"),
            GeometryParseRequest.defaults());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals(code)));
  }

  private static String json(String version, String bone, String extra) {
    return "{\"format_version\":\""
        + version
        + "\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\""
        + bone
        + "\""
        + extra
        + ",\"cubes\":[]}]}]}";
  }

  private static String geometry(String id, String bone) {
    return "{\"description\":{\"identifier\":\""
        + id
        + "\"},\"bones\":[{\"name\":\""
        + bone
        + "\",\"cubes\":[]}] }";
  }
}
