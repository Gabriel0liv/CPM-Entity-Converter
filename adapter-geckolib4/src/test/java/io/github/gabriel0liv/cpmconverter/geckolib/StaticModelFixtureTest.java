package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixture-only smoke coverage for the T201 static assembly boundary. */
class StaticModelFixtureTest {
  @Test
  void assemblesAllAuthorialFixturesWithoutAnimationInput() {
    Path root = Path.of("..", "test-fixtures").normalize();
    var parser = new GeckoGeometryParser();
    var assembler = new GeckoStaticModelAssembler();
    for (String name :
        List.of(
            "fixture-a-humanoid",
            "fixture-b-neck",
            "fixture-c-deep-hierarchy",
            "fixture-d-quadruped")) {
      Path directory = root.resolve(name);
      var geometry =
          parser.parse(directory.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
      assertTrue(geometry.success(), name + " geometry: " + geometry.diagnostics().all());
      var result =
          assembler.assemble(
              geometry.value(),
              directory.resolve("texture.png"),
              StaticModelAssemblyRequest.defaults());
      assertTrue(result.success(), name + " static model: " + result.diagnostics().all());
      assertTrue(result.value().clips().isEmpty(), name + " must not parse animation clips");
    }
  }
}
