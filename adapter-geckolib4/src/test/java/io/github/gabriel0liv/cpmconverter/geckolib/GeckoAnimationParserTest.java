package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeckoAnimationParserTest {
  @TempDir Path temp;

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

  @Test
  void mapsPlaybackComponentsAndTrackOrder() throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("channels.animation.json");
    Files.writeString(
        input,
        "{\"format_version\":\"1.8.0\",\"animations\":{\"z\":{\"animation_length\":1,\"loop\":\"loop\",\"bones\":{\"head\":{\"position\":{\"0.5\":[1,2,3]},\"scale\":[1,2,0.5],\"rotation\":[190,0,720]},\"body\":{\"rotation\":[0,0,0]}}}}}");
    var result = parse(model, input);
    assertTrue(result.success(), String.valueOf(result.diagnostics().all()));
    var clip = result.value().get(0);
    assertEquals(PlaybackMode.LOOP, clip.playback());
    assertEquals(1.0, clip.duration());
    assertEquals("body", model.bones().get(0).name());
    assertEquals(model.bones().get(0).id(), clip.tracks().get(0).bone());
    var head = clip.tracks().get(1);
    assertEquals("position", head.position().component());
    assertEquals(TransformMode.ADDITIVE, head.position().mode());
    assertEquals("scale", head.scale().component());
    assertEquals(TransformMode.ABSOLUTE, head.scale().mode());
    assertEquals(-1.0, head.position().keyframes().get(0).incomingValue().x());
    assertEquals(190.0, head.rotation().keyframes().get(0).incomingValue().x());
  }

  @Test
  void rejectsStructuralValuesAndDefersEasing() throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("invalid.animation.json");
    Files.writeString(
        input,
        "{\"format_version\":\"1.8.0\",\"animations\":{\"bad\":{\"bones\":{\"head\":{\"position\":[1,2],\"rotation\":{\"0\":{\"vector\":[0,0,0],\"easing\":\"step\"}}}}}}}");
    var result = parse(model, input);
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.ANIM_CHANNEL_INVALID)));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(
                d -> d.code().value().equals(DiagnosticCodes.ANIM_CUSTOM_EASING_UNSUPPORTED)));
  }

  private ModelIR staticModel(String fixture) throws Exception {
    Path dir = Path.of("..", "test-fixtures", fixture).normalize();
    var geometry =
        new GeckoGeometryParser()
            .parse(dir.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
    assertTrue(geometry.success(), String.valueOf(geometry.diagnostics().all()));
    var model =
        new GeckoStaticModelAssembler()
            .assemble(
                geometry.value(),
                dir.resolve("texture.png"),
                StaticModelAssemblyRequest.defaults());
    assertTrue(model.success(), String.valueOf(model.diagnostics().all()));
    return model.value();
  }

  private static Result<List<AnimationClipIR>> parse(ModelIR model, Path input) {
    return new GeckoAnimationParser()
        .parse(
            List.of(
                new AnimationInput(
                    input,
                    new io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath(
                        "fixtures/test.animation.json"))),
            model,
            AnimationParseRequest.defaults());
  }
}
