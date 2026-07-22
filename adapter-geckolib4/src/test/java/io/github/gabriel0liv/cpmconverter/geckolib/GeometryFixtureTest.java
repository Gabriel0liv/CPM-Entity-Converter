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
          var expected = golden.path("bones").get(index);
          var observed = result.value().bones().get(index);
          assertEquals(expected.path("name").asText(), observed.sourceName(), name);
          assertEquals(expected.path("id").asText(), observed.id().value(), name);
          assertEquals(
              expected.path("parent").isNull() ? null : expected.path("parent").asText(),
              observed.parent() == null ? null : observed.parent().value(),
              name);
          assertEquals(
              java.util.stream.StreamSupport.stream(expected.path("children").spliterator(), false)
                  .map(com.fasterxml.jackson.databind.JsonNode::asText)
                  .toList(),
              observed.children().stream().map(id -> id.value()).toList(),
              name);
          var translation = expected.path("bindTranslation");
          assertEquals(
              translation.get(0).asDouble(), observed.bindLocal().translation().x(), 1e-9, name);
          assertEquals(
              translation.get(1).asDouble(), observed.bindLocal().translation().y(), 1e-9, name);
          assertEquals(
              translation.get(2).asDouble(),
              observed.bindLocal().translation().z(),
              1e-9,
              name + ":" + observed.sourceName());
          assertEquals(expected.path("cubeCount").asInt(), observed.cubes().size(), name);
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
