package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeckoAnimationParserTest {
  @Test
  void parsesAuthorialFixturesAndAttachesClips() {
    Path root = Path.of("..", "test-fixtures").normalize();
    var geometryParser = new GeckoGeometryParser();
    var staticAssembler = new GeckoStaticModelAssembler();
    var animationParser = new GeckoAnimationParser();
    var animatedAssembler = new GeckoAnimatedModelAssembler();
    for (String name :
        List.of(
            "fixture-a-humanoid",
            "fixture-b-neck",
            "fixture-c-deep-hierarchy",
            "fixture-d-quadruped")) {
      Path dir = root.resolve(name);
      var geometry =
          geometryParser.parse(dir.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
      assertTrue(geometry.success(), name + geometry.diagnostics().all());
      var model =
          staticAssembler.assemble(
              geometry.value(), dir.resolve("texture.png"), StaticModelAssemblyRequest.defaults());
      assertTrue(model.success(), name + model.diagnostics().all());
      var clips =
          animationParser.parse(
              List.of(
                  new AnimationInput(
                      dir.resolve("animations.animation.json"),
                      new io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath(
                          "fixtures/" + name + "/animations.animation.json"))),
              model.value(),
              AnimationParseRequest.defaults());
      assertTrue(clips.success(), name + clips.diagnostics().all());
      assertFalse(clips.value().isEmpty(), name);
      var attached = animatedAssembler.attach(model.value(), clips.value());
      assertTrue(attached.success(), name + attached.diagnostics().all());
      assertEquals(clips.value().size(), attached.value().clips().size());
    }
  }
}
