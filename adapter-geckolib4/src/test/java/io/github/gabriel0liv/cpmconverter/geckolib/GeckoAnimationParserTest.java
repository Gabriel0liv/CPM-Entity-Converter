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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
  void assertsStructuredFixtureCAndDContracts() throws Exception {
    Path root = Path.of("..", "test-fixtures").normalize();
    var cModel = staticModel("fixture-c-deep-hierarchy");
    var c = parse(cModel, root.resolve("fixture-c-deep-hierarchy/animations.animation.json"));
    assertTrue(c.success(), String.valueOf(c.diagnostics().all()));
    var cClip = c.value().get(0);
    assertEquals("idle", cClip.id().value());
    assertEquals(PlaybackMode.LOOP, cClip.playback());
    assertEquals(1.0, cClip.duration());
    assertEquals(5, cClip.tracks().size());
    assertEquals(
        List.of("chest", "neck", "head", "jaw", "accessory"),
        cClip.tracks().stream()
            .map(
                t ->
                    cModel.bones().stream()
                        .filter(b -> b.id().equals(t.bone()))
                        .findFirst()
                        .orElseThrow()
                        .name())
            .toList());
    var accessory = cClip.tracks().get(4);
    assertNotNull(accessory.rotation());
    assertEquals(0.0, accessory.rotation().keyframes().get(1).outgoingValue().x());
    assertEquals(0.0, accessory.rotation().keyframes().get(1).outgoingValue().y());
    assertEquals(8.0, accessory.rotation().keyframes().get(1).outgoingValue().z());
    assertEquals(RotationOrder.ZYX, accessory.rotation().rotationOrder());
    assertNull(accessory.position());
    assertNull(accessory.scale());
    assertTrue(cClip.events().isEmpty());

    var dModel = staticModel("fixture-d-quadruped");
    var d = parse(dModel, root.resolve("fixture-d-quadruped/animations.animation.json"));
    assertTrue(d.success(), String.valueOf(d.diagnostics().all()));
    var dClip = d.value().get(0);
    assertEquals("walk", dClip.id().value());
    assertEquals(PlaybackMode.LOOP, dClip.playback());
    assertEquals(0.5, dClip.duration());
    assertEquals(1, dClip.tracks().size());
    var leg = dClip.tracks().get(0);
    assertEquals(
        "leg_fl",
        dModel.bones().stream()
            .filter(b -> b.id().equals(leg.bone()))
            .findFirst()
            .orElseThrow()
            .name());
    assertNotNull(leg.rotation());
    assertEquals(0.0, leg.rotation().keyframes().get(0).timeSeconds());
    assertEquals(0.5, leg.rotation().keyframes().get(1).timeSeconds());
    assertEquals(20.0, leg.rotation().keyframes().get(0).outgoingValue().x());
    assertEquals(-20.0, leg.rotation().keyframes().get(1).outgoingValue().x());
    assertTrue(dClip.events().isEmpty());
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

  @ParameterizedTest
  @CsvSource({
    "loop,LOOP",
    "true,LOOP",
    "false,PLAY_ONCE",
    "play_once,PLAY_ONCE",
    "hold_on_last_frame,HOLD"
  })
  void mapsSupportedPlaybackModes(String authored, PlaybackMode expected) throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("playback-" + authored + ".json");
    Files.writeString(input, animationWith("\"loop\":\"" + authored + "\""));
    var result = parse(model, input);
    assertTrue(result.success(), String.valueOf(result.diagnostics().all()));
    assertEquals(expected, result.value().get(0).playback());
  }

  @Test
  void classifiesMolangComponentsSeparatelyFromStructuralErrors() throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("molang-components.json");
    Files.writeString(
        input,
        "{\"format_version\":\"1.8.0\",\"animations\":{\"bad\":{\"bones\":{\"head\":{"
            + "\"position\":[\"query.anim_time\",0,0],\"rotation\":[0,true,0]}}}}}");
    var result = parse(model, input);
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(
                d -> d.code().value().equals(DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED)));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.ANIM_CHANNEL_INVALID)));
  }

  @Test
  void equivalentPrePostVectorFormsDoNotWarn() throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("pre-post-equivalent.json");
    Files.writeString(
        input,
        "{\"format_version\":\"1.8.0\",\"animations\":{\"idle\":{\"animation_length\":1,\"bones\":{\"head\":{"
            + "\"rotation\":{\"0\":{\"pre\":[1,2,3],\"post\":{\"vector\":[1,2,3]}}}}}}}}");
    var result = parse(model, input);
    assertTrue(result.success(), String.valueOf(result.diagnostics().all()));
    assertTrue(
        result.diagnostics().all().stream()
            .noneMatch(d -> d.code().value().equals(DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449)));
  }

  @Test
  void differingPrePostStillWarnsAndUsesPre() throws Exception {
    var model = staticModel("fixture-a-humanoid");
    Path input = temp.resolve("pre-post-different.json");
    Files.writeString(
        input,
        "{\"format_version\":\"1.8.0\",\"animations\":{\"idle\":{\"animation_length\":1,\"bones\":{\"head\":{"
            + "\"rotation\":{\"0\":{\"pre\":{\"vector\":[1,2,3]},\"post\":[4,5,6]}}}}}}}");
    var result = parse(model, input);
    assertTrue(result.success(), String.valueOf(result.diagnostics().all()));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449)));
    var key = result.value().get(0).tracks().get(0).rotation().keyframes().get(0);
    assertEquals(1.0, key.incomingValue().x());
  }

  private static String animationWith(String loopField) {
    return "{\"format_version\":\"1.8.0\",\"animations\":{\"idle\":{"
        + loopField
        + ",\"animation_length\":1,\"bones\":{\"head\":{\"rotation\":[0,0,0]}}}}}";
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
