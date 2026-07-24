package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectIoUvConformanceTest {
  @Test
  void loadedTexturedElementsExposeUvAndTextureFields() throws Exception {
    for (String fixture :
        new String[] {
          "fixture-a-humanoid", "fixture-b-neck", "fixture-c-deep-hierarchy", "fixture-d-quadruped"
        }) {
      var elements = T304ConformanceTestSupport.project(fixture).getAsJsonArray("elements");
      boolean textured = false;
      boolean sized = false;
      for (var element : elements) {
        textured |= element.getAsJsonObject().get("texture").getAsBoolean();
        sized |= element.getAsJsonObject().get("textureSize").getAsInt() > 0;
      }
      assertTrue(textured, fixture);
      assertTrue(sized, fixture);
    }
  }
}
