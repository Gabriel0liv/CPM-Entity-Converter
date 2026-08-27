package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoGeometryParserIntegrationTest {
  @Test
  void parsesFixtureAHumanoidInSourceOrder() {
    Path fixture =
        Path.of("..", "test-fixtures", "fixture-a-humanoid", "geometry.geo.json")
            .toAbsolutePath()
            .normalize();

    var result = new GeckoGeometryParser().parse(fixture);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var model = result.value();
    assertEquals("cpm:a", model.geometryId().value());
    assertEquals(6, model.bones().size());
    assertEquals(
        java.util.List.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"),
        model.bones().stream().map(bone -> bone.name()).toList());
    assertEquals(
        java.util.List.of("body"), model.roots().stream().map(root -> root.value()).toList());
    assertVec(new Vec3d(0, -8, 0), model.bones().get(1).bind().translation());
    assertVec(new Vec3d(-3, -7, 0), model.bones().get(2).bind().translation());
    assertEquals(1, model.bones().get(0).cubes().size());
  }

  @Test
  void rejectsHierarchyCycleBeforeBuildingIr() throws Exception {
    Path input =
        geometry(
            """
            {
              "format_version":"1.12.0",
              "minecraft:geometry":[{
                "description":{"identifier":"demo:cycle","texture_width":16,"texture_height":16},
                "bones":[
                  {"name":"a","parent":"b","pivot":[0,0,0]},
                  {"name":"b","parent":"a","pivot":[0,1,0]}
                ]
              }]
            }
            """);

    var result = new GeckoGeometryParser().parse(input);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.GEO_HIERARCHY_CYCLE);
  }

  @Test
  void rejectsMalformedVectorsAndNegativeCubeSize() throws Exception {
    Path badPivot =
        geometry(
            """
            {
              "format_version":"1.12.0",
              "minecraft:geometry":[{
                "description":{"identifier":"demo:bad-pivot","texture_width":16,"texture_height":16},
                "bones":[{"name":"body","pivot":[0,1]}]
              }]
            }
            """);
    Path badSize =
        geometry(
            """
            {
              "format_version":"1.12.0",
              "minecraft:geometry":[{
                "description":{"identifier":"demo:bad-size","texture_width":16,"texture_height":16},
                "bones":[{
                  "name":"body",
                  "pivot":[0,0,0],
                  "cubes":[{"origin":[0,0,0],"size":[1,-1,1],"uv":[0,0]}]
                }]
              }]
            }
            """);

    var pivotResult = new GeckoGeometryParser().parse(badPivot);
    var sizeResult = new GeckoGeometryParser().parse(badSize);

    assertFalse(pivotResult.success());
    assertHasCode(pivotResult, DiagnosticCodes.GEO_INVALID_VALUE);
    assertFalse(sizeResult.success());
    assertHasCode(sizeResult, DiagnosticCodes.GEO_INVALID_VALUE);
  }

  private static Path geometry(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-integration-", ".geo.json");
    Files.writeString(path, json);
    return path;
  }

  private static void assertVec(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x(), actual.x(), 1e-9);
    assertEquals(expected.y(), actual.y(), 1e-9);
    assertEquals(expected.z(), actual.z(), 1e-9);
  }

  private static void assertHasCode(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result, String code) {
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> "missing diagnostic " + code + " in " + result.diagnostics().all());
  }
}
