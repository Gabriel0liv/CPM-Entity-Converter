package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.gabriel0liv.cpmconverter.ir.CubeFaceIR;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class PerFaceUvOrderTest {
  @Test
  void preservesEnumOrderAndDefensiveCopy() {
    var input = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
    input.put(CubeFaceIR.DOWN, new FaceUvIR(0, 0, 1, 1));
    input.put(CubeFaceIR.NORTH, new FaceUvIR(0, 0, 1, 1));
    var value = new PerFaceUvIR(input);
    input.clear();
    assertEquals(
        java.util.List.of(CubeFaceIR.NORTH, CubeFaceIR.DOWN),
        value.faces().keySet().stream().toList());
    assertThrows(UnsupportedOperationException.class, () -> value.faces().clear());
  }
}
