package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformSpace;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoAnimationParserTest {
  @Test
  void parsesFixtureAInSourceOrderAndPreservesAuthoredEulerDegrees() {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    var result =
        new GeckoAnimationParser()
            .parse(fixture.resolve("animations.animation.json"), geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(2, result.value().size());

    var idle = result.value().get(0);
    assertEquals("idle", idle.id().value());
    assertEquals(1.0, idle.duration(), 1e-12);
    assertEquals(PlaybackMode.LOOP, idle.playback());
    assertEquals(1, idle.tracks().size());
    assertEquals("head", idle.tracks().get(0).bone().value());
    var idleRotation = idle.tracks().get(0).rotation();
    assertEquals(2, idleRotation.keyframes().size());
    assertEquals(new Vec3d(0, 0, 0), idleRotation.keyframes().get(0).incomingValue());
    assertEquals(new Vec3d(0, 2, 0), idleRotation.keyframes().get(1).incomingValue());
    assertEquals(
        idleRotation.keyframes().get(1).incomingValue(),
        idleRotation.keyframes().get(1).outgoingValue());

    var walk = result.value().get(1);
    assertEquals("walk", walk.id().value());
    assertEquals(0.5, walk.duration(), 1e-12);
    assertEquals(PlaybackMode.LOOP, walk.playback());
    assertEquals(4, walk.tracks().size());
    assertEquals("left_arm", walk.tracks().get(0).bone().value());
    assertEquals(
        new Vec3d(-15, 0, 0), walk.tracks().get(0).rotation().keyframes().get(0).incomingValue());
    assertEquals(
        new Vec3d(15, 0, 0), walk.tracks().get(0).rotation().keyframes().get(1).incomingValue());
  }

  @Test
  void parsesPlaybackPositionScaleDefaultsAndAuthoredWinding() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation =
        animation(
            """
            {
              "format_version":"1.8.0",
              "animations":{
                "once_false":{
                  "animation_length":1.0,
                  "loop":false,
                  "bones":{"body":{
                    "position":[2,3],
                    "rotation":[720,0,0],
                    "scale":[2,3]
                  }}
                },
                "once_string":{
                  "animation_length":1.0,
                  "loop":"play_once",
                  "bones":{"body":{"rotation":[0,0,0]}}
                },
                "hold":{
                  "animation_length":1.0,
                  "loop":"hold_on_last_frame",
                  "bones":{"body":{"rotation":[0,0,0]}}
                },
                "custom":{
                  "animation_length":1.0,
                  "loop":"mod:conditional_loop",
                  "bones":{"body":{"rotation":[0,0,0]}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(4, result.value().size());
    assertEquals(PlaybackMode.PLAY_ONCE, result.value().get(0).playback());
    assertEquals(PlaybackMode.PLAY_ONCE, result.value().get(1).playback());
    assertEquals(PlaybackMode.HOLD, result.value().get(2).playback());
    assertEquals(PlaybackMode.CUSTOM, result.value().get(3).playback());
    assertEquals("mod:conditional_loop", result.value().get(3).customLoop());

    var track = result.value().get(0).tracks().get(0);
    assertEquals(TransformSpace.LOCAL, track.space());
    assertEquals(TransformMode.ADDITIVE, track.mode());

    assertEquals(TransformMode.ADDITIVE, track.position().mode());
    assertEquals(TransformSpace.LOCAL, track.position().space());
    assertEquals(new Vec3d(2, 3, 0), track.position().keyframes().get(0).incomingValue());

    assertEquals(TransformMode.ABSOLUTE, track.scale().mode());
    assertEquals(TransformSpace.LOCAL, track.scale().space());
    assertEquals(new Vec3d(2, 3, 1), track.scale().keyframes().get(0).incomingValue());

    assertEquals(
        new Vec3d(720, 0, 0), track.rotation().keyframes().get(0).incomingValue());
  }

  private static Path animation(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-animation-", ".animation.json");
    Files.writeString(path, json);
    return path;
  }
}
