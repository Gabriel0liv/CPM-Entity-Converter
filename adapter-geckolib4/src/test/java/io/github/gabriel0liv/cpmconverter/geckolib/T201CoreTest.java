package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class T201CoreTest {
  @Test
  void validatesAuthorialPngWithoutChangingBytes() throws Exception {
    Path png = Path.of("../test-fixtures/fixture-a-humanoid/texture.png");
    byte[] before = java.nio.file.Files.readAllBytes(png);
    var result = new PngTextureValidator().validate(png, PngValidationRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals(32, result.value().width());
    assertEquals(32, result.value().height());
    assertTrue(java.util.Arrays.equals(before, java.nio.file.Files.readAllBytes(png)));
  }

  @Test
  void decodesFractionalAndSignedBoxUv() {
    var source = new SourcePath("fixture.geo.json");
    var cube =
        new ParsedCube(
            new io.github.gabriel0liv.cpmconverter.ir.CubeId("g/cube/0"),
            new io.github.gabriel0liv.cpmconverter.ir.BoneId("g/bone/0"),
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(0, 0, 0),
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(2, 2, 2),
            io.github.gabriel0liv.cpmconverter.math.Vec3d.ZERO,
            io.github.gabriel0liv.cpmconverter.math.Vec3d.ZERO,
            0,
            false,
            new RawUvBoundary("[-1.5,2.25]"),
            io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation.of(source));
    var result = new GeckoUvDecoder().decode(cube.rawUv(), cube, 32, 32);
    assertTrue(result.success(), result.diagnostics().all().toString());
    var box = (BoxUvIR) result.value();
    assertEquals(-1.5, box.u());
    assertEquals(2.25, box.v());
  }
}
