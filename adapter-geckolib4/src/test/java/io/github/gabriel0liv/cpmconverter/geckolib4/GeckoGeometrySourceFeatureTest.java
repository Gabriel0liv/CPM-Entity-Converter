package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeckoGeometrySourceFeatureTest {
  @Test
  void preservesNeverRenderAsOwnCubeVisibilityWithoutHidingChildren() throws Exception {
    var result =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:never-render","texture_width":16,"texture_height":16},
                        "bones":[
                          {
                            "name":"body",
                            "pivot":[0,24,0],
                            "neverRender":true,
                            "cubes":[{"origin":[-1,22,-1],"size":[2,2,2],"uv":[0,0]}]
                          },
                          {
                            "name":"child",
                            "parent":"body",
                            "pivot":[0,22,0],
                            "cubes":[{"origin":[-1,20,-1],"size":[2,2,2],"uv":[0,0]}]
                          }
                        ]
                      }]
                    }
                    """));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    BoneIR body = result.value().bones().get(0);
    BoneIR child = result.value().bones().get(1);

    assertFalse(body.renderOwnCubes());
    assertTrue(child.renderOwnCubes());
    assertEquals(1, body.cubes().size(), "neverRender must not discard source geometry");
    assertEquals(List.of(child.id()), body.children(), "descendants must remain in the hierarchy");
  }

  @Test
  void matchesBuiltinInflateAndMirrorSemantics() throws Exception {
    var result =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:inheritance","texture_width":16,"texture_height":16},
                        "bones":[{
                          "name":"body",
                          "pivot":[0,24,0],
                          "inflate":0.5,
                          "mirror":true,
                          "cubes":[
                            {"origin":[0,0,0],"size":[1,1,1],"uv":[0,0]},
                            {"origin":[1,0,0],"size":[1,1,1],"inflate":0.25,"mirror":true,"uv":[0,0]}
                          ]
                        }]
                      }]
                    }
                    """));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var inherited = result.value().bones().get(0).cubes().get(0);
    var overridden = result.value().bones().get(0).cubes().get(1);

    assertEquals(0.5, inherited.inflate(), 1e-12);
    assertFalse(inherited.mirror(), "bone mirror is not inherited by Builtin.constructCube");
    assertEquals(0.25, overridden.inflate(), 1e-12);
    assertTrue(overridden.mirror());
  }

  @Test
  void rejectsTextureMeshesInsteadOfDroppingThem() throws Exception {
    var result =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{"identifier":"demo:texture-mesh","texture_width":16,"texture_height":16},
                        "bones":[{
                          "name":"body",
                          "pivot":[0,24,0],
                          "texture_meshes":[{"texture":"default","position":[0,0,0],"rotation":[0,0,0],"local_pivot":[0,0,0]}]
                        }]
                      }]
                    }
                    """));

    assertFalse(result.success());
    assertHasCode(result.diagnostics().all(), DiagnosticCodes.GEO_MESH_UNSUPPORTED);
  }

  @Test
  void recordsRecognizedBuiltinMetadataInsteadOfSilentlyDroppingIt() throws Exception {
    var result =
        new GeckoGeometryParser()
            .parse(
                geometry(
                    """
                    {
                      "format_version":"1.12.0",
                      "minecraft:geometry":[{
                        "description":{
                          "identifier":"demo:metadata",
                          "texture_width":16,
                          "texture_height":16,
                          "visible_bounds_width":3,
                          "visible_bounds_height":4,
                          "visible_bounds_offset":[0,1,0],
                          "preserve_model_pose":true,
                          "animationNoHeadBob":true
                        },
                        "bones":[{
                          "name":"body",
                          "pivot":[0,24,0],
                          "mirror":true,
                          "reset":true,
                          "debug":true,
                          "bind_pose_rotation":[1,2,3],
                          "locators":{"hand":[1,2,3]},
                          "render_group_id":1
                        }]
                      }]
                    }
                    """));

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var features = result.value().unsupportedFeatures();
    var names = features.stream().map(feature -> feature.feature()).toList();

    assertEquals(
        List.of(
            "gecko.model.animationNoHeadBob",
            "gecko.model.preserve_model_pose",
            "gecko.model.visible_bounds_height",
            "gecko.model.visible_bounds_offset",
            "gecko.model.visible_bounds_width",
            "gecko.bone.bind_pose_rotation",
            "gecko.bone.debug",
            "gecko.bone.locators",
            "gecko.bone.mirror",
            "gecko.bone.render_group_id",
            "gecko.bone.reset"),
        names);
    assertTrue(features.stream().allMatch(feature -> feature.source() != null));
  }

  private static Path geometry(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-source-features-", ".geo.json");
    Files.writeString(path, json);
    return path;
  }

  private static void assertHasCode(
      java.util.List<io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic> diagnostics,
      String code) {
    assertTrue(
        diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> "missing diagnostic " + code + " in " + diagnostics);
  }
}
