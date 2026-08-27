package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoBoxUvPrecisionTest {
  @Test
  void preservesFractionalBoxUvCoordinates() throws Exception {
    Path geometry = Files.createTempFile("cpm-converter-box-uv-", ".geo.json");
    Files.writeString(
        geometry,
        """
        {
          "format_version":"1.12.0",
          "minecraft:geometry":[{
            "description":{"identifier":"demo:fractional-uv","texture_width":32,"texture_height":32},
            "bones":[{
              "name":"body",
              "pivot":[0,0,0],
              "cubes":[{
                "origin":[0,0,0],
                "size":[2,2,2],
                "uv":[1.5,2.25]
              }]
            }]
          }]
        }
        """);

    var result = new GeckoGeometryParser().parse(geometry);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    BoxUvIR uv = assertInstanceOf(BoxUvIR.class, result.value().bones().get(0).cubes().get(0).uv());
    assertEquals(1.5, uv.u(), 1e-12);
    assertEquals(2.25, uv.v(), 1e-12);
  }
}
