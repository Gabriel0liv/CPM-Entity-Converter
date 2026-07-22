package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeometryFixtureTest {
  @Test
  void comparesCompleteGeometryOnlySnapshots() throws Exception {
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
      var expected =
          mapper.readTree(root.resolve(name).resolve("expected/geometry-parsed.json").toFile());
      var observed = GeometryParsedSnapshot.write(result.value());
      assertEquals(expected, observed, name);
    }
  }
}
