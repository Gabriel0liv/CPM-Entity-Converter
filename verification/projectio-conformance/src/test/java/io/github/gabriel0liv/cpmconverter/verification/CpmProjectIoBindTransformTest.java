package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectIoBindTransformTest {
  @Test
  void loadedHierarchyContainsStableLocalBindTransforms() throws Exception {
    for (String fixture :
        new String[] {
          "fixture-a-humanoid", "fixture-b-neck", "fixture-c-deep-hierarchy", "fixture-d-quadruped"
        }) {
      var elements = T304ConformanceTestSupport.project(fixture).getAsJsonArray("elements");
      boolean transforms = true;
      boolean child = false;
      for (var element : elements) {
        transforms &=
            element.getAsJsonObject().has("position") && element.getAsJsonObject().has("rotation");
        child |= !element.getAsJsonObject().get("parentPath").getAsString().isEmpty();
      }
      assertTrue(transforms, fixture);
      assertTrue(child, fixture);
    }
  }
}
