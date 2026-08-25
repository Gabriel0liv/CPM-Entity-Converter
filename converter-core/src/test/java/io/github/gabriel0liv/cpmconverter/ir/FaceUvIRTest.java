package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FaceUvIRTest {
  @Test
  void preservesSignedUvExtentsBecauseTheyEncodeOrientation() {
    FaceUvIR uv = new FaceUvIR(12, 20, -4, 6);

    assertEquals(12, uv.u());
    assertEquals(20, uv.v());
    assertEquals(-4, uv.width());
    assertEquals(6, uv.height());
  }
}
