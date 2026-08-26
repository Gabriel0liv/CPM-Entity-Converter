package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoGeometryParserTest {
  @Test
  void parsesSingleGeometryHierarchyAndEffectiveCubeFields() throws Exception {
    Path input =
        geometry(
            """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [{
                "description": {
                  "identifier": "demo:model",
                  "texture_width": 32,
                  "texture_height": 32
                },
                "bones": [
                  {
                    "name": "body",
                    "pivot": [1, 2, 3],
                    "inflate": 0.5,
                    "cubes": [{
                      "origin": [-2, 4, 6],
                      "size": [4, 5, 6],
                      "mirror": true,
                      "uv": [3, 7]
                    }]
                  },
                  {
                    "name": "head",
                    "parent": "body",
                    "pivot": [3, 5, 7],
                    "rotation": [10, -20, 30]
                  }
                ]
              }]
            }
            """);

    var result = new GeckoGeometryParser().parse(input);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    ModelIR model = result.value();
    assertEquals("demo:model", model.geometryId().value());
    assertEquals(2, model.bones().size());
    assertEquals("body", model.bones().get(0).name());
    assertEquals("head", model.bones().get(1).name());
    assertEquals(1, model.roots().size());
    assertEquals("body", model.roots().get(0).value());

    BoneIR body = model.bones().get(0);
    BoneIR head = model.bones().get(1);
    assertEquals(head.id(), body.children().get(0));
    assertEquals(body.id(), head.parent());
    assertVec(new Vec3d(-1, -2, 3), body.bind().translation());
    assertVec(new Vec3d(-2, -3, 4), head.bind().translation());
    assertRotation(quaternionFromDegrees(new Vec3d(-10, 20, 30)), head.bind().rotation());

    assertEquals(1, body.cubes().size());
    var cube = body.cubes().get(0);
    assertEquals(0.5, cube.inflate(), 1e-12);
    assertTrue(cube.mirror());
    BoxUvIR uv = assertInstanceOf(BoxUvIR.class, cube.uv());
    assertEquals(3, uv.u());
    assertEquals(7, uv.v());
    assertVec(new Vec3d(-1, -7, 3), cube.origin());
  }

  @Test
  void preservesCubeOwnPivotAndRotationWithoutFlattening() throws Exception {
    Path input =
        geometry(
            """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [{
                "description": {
                  "identifier": "demo:cube-pivot",
                  "texture_width": 32,
                  "texture_height": 32
                },
                "bones": [{
                  "name": "body",
                  "pivot": [2, 4, 6],
                  "cubes": [{
                    "origin": [1, 2, 3],
                    "size": [2, 2, 2],
                    "pivot": [3, 5, 7],
                    "rotation": [10, 20, 30],
                    "uv": [0, 0]
                  }]
                }]
              }]
            }
            """);

    var result = new GeckoGeometryParser().parse(input);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var cube = result.value().bones().get(0).cubes().get(0);
    assertVec(new Vec3d(-1, -1, 1), cube.pivot());
    assertVec(new Vec3d(0, 1, -4), cube.origin());
    assertRotation(quaternionFromDegrees(new Vec3d(-10, -20, 30)), cube.rotation());
  }

  @Test
  void selectsExactGeometryAndRejectsAmbiguousOrMissingSelection() throws Exception {
    Path input =
        geometry(
            """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [
                {
                  "description": {"identifier": "demo:a", "texture_width": 16, "texture_height": 16},
                  "bones": [{"name": "a", "pivot": [0, 0, 0]}]
                },
                {
                  "description": {"identifier": "demo:b", "texture_width": 16, "texture_height": 16},
                  "bones": [{"name": "b", "pivot": [0, 0, 0]}]
                }
              ]
            }
            """);

    var ambiguous = new GeckoGeometryParser().parse(input);
    assertFalse(ambiguous.success());
    assertHasCode(ambiguous.diagnostics().all(), DiagnosticCodes.GEO_MULTIPLE_MODELS);

    var selected = new GeckoGeometryParser().parse(input, "demo:b");
    assertTrue(selected.success(), () -> selected.diagnostics().all().toString());
    assertEquals("demo:b", selected.value().geometryId().value());
    assertEquals("b", selected.value().bones().get(0).name());

    var missing = new GeckoGeometryParser().parse(input, "demo:missing");
    assertFalse(missing.success());
    assertHasCode(missing.diagnostics().all(), DiagnosticCodes.GEO_MODEL_NOT_FOUND);
  }

  @Test
  void rejectsUnsupportedVersionDuplicateBoneMissingParentAndMesh() throws Exception {
    var unsupported =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {"format_version":"1.21.0","minecraft:geometry":[]}
                    """));
    assertFalse(unsupported.success());
    assertHasCode(unsupported.diagnostics().all(), DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);

    var duplicate =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:duplicate","texture_width":16,"texture_height":16},
                        "bones":[
                          {"name":"body","pivot":[0,0,0]},
                          {"name":"body","pivot":[0,1,0]}
                        ]
                      }]
                    }
                    """));
    assertFalse(duplicate.success());
    assertHasCode(duplicate.diagnostics().all(), DiagnosticCodes.GEO_DUPLICATE_BONE_NAME);

    var missingParent =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:parent","texture_width":16,"texture_height":16},
                        "bones":[{"name":"head","parent":"missing","pivot":[0,1,0]}]
                      }]
                    }
                    """));
    assertFalse(missingParent.success());
    assertHasCode(missingParent.diagnostics().all(), DiagnosticCodes.GEO_PARENT_NOT_FOUND);

    var mesh =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:mesh","texture_width":16,"texture_height":16},
                        "bones":[{"name":"body","pivot":[0,0,0],"poly_mesh":{}}]
                      }]
                    }
                    """));
    assertFalse(mesh.success());
    assertHasCode(mesh.diagnostics().all(), DiagnosticCodes.GEO_MESH_UNSUPPORTED);
  }

  private static Path geometry(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-geometry-", ".geo.json");
    Files.writeString(path, json);
    return path;
  }

  private static Quatd quaternionFromDegrees(Vec3d degrees) {
    return Quatd.fromEulerZYX(
        Math.toRadians(degrees.x()), Math.toRadians(degrees.y()), Math.toRadians(degrees.z()));
  }

  private static void assertRotation(Quatd expected, Quatd actual) {
    assertVec(expected.rotate(Vec3d.X), actual.rotate(Vec3d.X));
    assertVec(expected.rotate(Vec3d.Y), actual.rotate(Vec3d.Y));
    assertVec(expected.rotate(Vec3d.Z), actual.rotate(Vec3d.Z));
  }

  private static void assertVec(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x(), actual.x(), 1e-9);
    assertEquals(expected.y(), actual.y(), 1e-9);
    assertEquals(expected.z(), actual.z(), 1e-9);
  }

  private static void assertHasCode(
      java.util.List<io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic> diagnostics,
      String code) {
    assertTrue(
        diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> "missing diagnostic " + code + " in " + diagnostics);
  }
}
