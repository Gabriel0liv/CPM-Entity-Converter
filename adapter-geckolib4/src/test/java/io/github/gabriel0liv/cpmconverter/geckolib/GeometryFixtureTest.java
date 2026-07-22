package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeometryFixtureTest {
  @Test
  void parsesAuthorialFixturesWithoutAnimationOrUvInterpretation() {
    Path root = Path.of("..", "test-fixtures").normalize();
    List<String> names =
        List.of(
            "fixture-a-humanoid",
            "fixture-b-neck",
            "fixture-c-deep-hierarchy",
            "fixture-d-quadruped");
    GeckoGeometryParser parser = new GeckoGeometryParser();
    ObjectMapper mapper = new ObjectMapper();
    for (String name : names) {
      var result =
          parser.parse(
              root.resolve(name).resolve("geometry.geo.json"), GeometryParseRequest.defaults());
      assertTrue(result.success(), name + ": " + result.diagnostics().all());
      assertEquals(1, result.value().roots().size(), name);
      assertTrue(result.value().bones().stream().allMatch(bone -> !bone.cubes().isEmpty()), name);
      try {
        var golden =
            mapper.readTree(root.resolve(name).resolve("expected/geometry-parsed.json").toFile());
        assertEquals(golden.path("geometryId").asText(), result.value().geometryId().value(), name);
        assertEquals(golden.path("bones").size(), result.value().bones().size(), name);
        for (int index = 0; index < result.value().bones().size(); index++) {
          assertEquals(
              golden.path("bones").get(index).path("name").asText(),
              result.value().bones().get(index).sourceName(),
              name);
        }
      } catch (java.io.IOException exception) {
        throw new AssertionError("invalid geometry golden for " + name, exception);
      }
    }
    var c =
        parser.parse(
            root.resolve("fixture-c-deep-hierarchy/geometry.geo.json"),
            GeometryParseRequest.defaults());
    var accessory =
        c.value().bones().stream()
            .filter(bone -> bone.sourceName().equals("accessory"))
            .findFirst()
            .orElseThrow();
    assertEquals(12, accessory.cubes().get(0).rotationDegrees().x(), 1e-9);
    assertEquals(27, accessory.cubes().get(0).rotationDegrees().z(), 1e-9);
  }
}
