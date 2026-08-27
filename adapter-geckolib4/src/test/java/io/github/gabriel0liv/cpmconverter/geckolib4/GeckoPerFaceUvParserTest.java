package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoPerFaceUvParserTest {
  @Test
  void parsesFixtureCPerFaceUvWithoutDroppingFaces() {
    Path geometry =
        Path.of("..", "test-fixtures", "fixture-c-deep-hierarchy", "geometry.geo.json").normalize();

    var result = new GeckoGeometryParser().parse(geometry);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var accessory =
        result.value().bones().stream()
            .filter(bone -> bone.name().equals("accessory"))
            .findFirst()
            .orElseThrow();
    PerFaceUvIR uv = assertInstanceOf(PerFaceUvIR.class, accessory.cubes().get(0).uv());
    assertEquals(6, uv.faces().size());
    assertEquals(new FaceUvIR(4, 0, 4, 4), uv.faces().get("down"));
    assertEquals(new FaceUvIR(16, 0, 4, 4), uv.faces().get("east"));
    assertEquals(new FaceUvIR(8, 0, 4, 4), uv.faces().get("north"));
    assertEquals(new FaceUvIR(12, 0, 4, 4), uv.faces().get("south"));
    assertEquals(new FaceUvIR(0, 0, 4, 4), uv.faces().get("up"));
    assertEquals(new FaceUvIR(20, 0, 4, 4), uv.faces().get("west"));
  }

  @Test
  void preservesSignedUvSizeBecauseGeckoUsesItForOrientation() throws Exception {
    Path geometry =
        geometry(
            """
            {
              "format_version":"1.12.0",
              "minecraft:geometry":[{
                "description":{"identifier":"demo:signed-uv","texture_width":32,"texture_height":32},
                "bones":[{
                  "name":"body",
                  "pivot":[0,0,0],
                  "cubes":[{
                    "origin":[0,0,0],
                    "size":[2,2,2],
                    "uv":{
                      "north":{"uv":[12,20],"uv_size":[-4,6]}
                    }
                  }]
                }]
              }]
            }
            """);

    var result = new GeckoGeometryParser().parse(geometry);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    PerFaceUvIR uv =
        assertInstanceOf(PerFaceUvIR.class, result.value().bones().get(0).cubes().get(0).uv());
    assertEquals(new FaceUvIR(12, 20, -4, 6), uv.faces().get("north"));
  }

  private static Path geometry(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-per-face-uv-", ".geo.json");
    Files.writeString(path, json);
    return path;
  }
}
