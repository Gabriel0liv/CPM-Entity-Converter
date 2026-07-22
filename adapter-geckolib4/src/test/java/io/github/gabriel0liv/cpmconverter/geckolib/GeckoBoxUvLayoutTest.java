package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeFaceIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import org.junit.jupiter.api.Test;

class GeckoBoxUvLayoutTest {
  @Test
  void derivesCanonicalSixFaceLayout() {
    var faces = GeckoBoxUvLayout.derive(new BoxUvIR(0, 0), new Vec3d(4, 5, 6));
    assertEquals(10, faces.get(CubeFaceIR.WEST).u());
    assertEquals(6, faces.get(CubeFaceIR.WEST).v());
    assertEquals(6, faces.get(CubeFaceIR.WEST).width());
    assertEquals(5, faces.get(CubeFaceIR.WEST).height());
    assertEquals(0, faces.get(CubeFaceIR.EAST).u());
    assertEquals(6, faces.get(CubeFaceIR.EAST).v());
    assertEquals(6, faces.get(CubeFaceIR.NORTH).u());
    assertEquals(16, faces.get(CubeFaceIR.SOUTH).u());
    assertEquals(6, faces.get(CubeFaceIR.UP).u());
    assertEquals(-6, faces.get(CubeFaceIR.DOWN).height());
  }

  @Test
  void floorsDecimalCubeDimensions() {
    var faces = GeckoBoxUvLayout.derive(new BoxUvIR(0, 0), new Vec3d(4.9, 5.9, 6.9));
    assertEquals(4, faces.get(CubeFaceIR.NORTH).width());
    assertEquals(5, faces.get(CubeFaceIR.NORTH).height());
    assertEquals(6, faces.get(CubeFaceIR.NORTH).v());
  }
}
