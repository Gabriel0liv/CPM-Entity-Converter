package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
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
                    "position":[2,3,4],
                    "rotation":[720,0,0],
                    "scale":[2,3,4]
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
    assertEquals(new Vec3d(-2, -3, 4), track.position().keyframes().get(0).incomingValue());

    assertEquals(TransformMode.ABSOLUTE, track.scale().mode());
    assertEquals(TransformSpace.LOCAL, track.scale().space());
    assertEquals(new Vec3d(2, 3, 4), track.scale().keyframes().get(0).incomingValue());

    assertEquals(
        new Vec3d(720, 0, 0), track.rotation().keyframes().get(0).incomingValue());
  }

  @Test
  void rejectsPartialAnimationVectorsLikeGecko449() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation =
        animation(
            """
            {
              "animations":{
                "partial":{
                  "animation_length":1.0,
                  "bones":{"body":{"position":[1,2]}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertFalse(result.success());
  }

  @Test
  void reproducesGecko449PreWinsAndReportsCollapsedPost() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation =
        animation(
            """
            {
              "format_version":"1.8.0",
              "animations":{
                "pre_post":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":{
                    "0.0":{"pre":[1,2,3],"post":[4,5,6]},
                    "0.5":{"post":[7,8,9]}
                  }}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var keyframes = result.value().get(0).tracks().get(0).rotation().keyframes();
    assertEquals(new Vec3d(1, 2, 3), keyframes.get(0).incomingValue());
    assertEquals(new Vec3d(1, 2, 3), keyframes.get(0).outgoingValue());
    assertEquals(new Vec3d(7, 8, 9), keyframes.get(1).incomingValue());
    assertTrue(
        result.diagnostics().warnings().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.ANIM_PRE_POST_COLLAPSED_449)));
  }

  @Test
  void recognizesIgnoredChannelLerpModeInsteadOfParsingItAsTimestamp() {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation =
        Path.of(
                "..",
                "spikes",
                "geckolib-animation-semantics",
                "fixtures",
                "LERP-001.json")
            .normalize();
    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var rotation = result.value().get(0).tracks().get(0).rotation();
    assertEquals(2, rotation.keyframes().size());
    assertEquals(new Vec3d(0, 0, 0), rotation.keyframes().get(0).incomingValue());
    assertEquals(new Vec3d(1, 1, 1), rotation.keyframes().get(1).incomingValue());
    assertTrue(
        result.diagnostics().warnings().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.ANIM_LERP_MODE_IGNORED_449)));
  }

  @Test
  void recordsOutOfScopeEventsInIrAndDiagnostics() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation =
        animation(
            """
            {
              "format_version":"1.8.0",
              "animations":{
                "events":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":[0,0,0]}},
                  "sound_effects":{"0.0":{"effect":"step"}},
                  "particle_effects":{"0.25":{"effect":"dust","locator":"body"}},
                  "timeline":{"0.5":"instruction"}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var clip = result.value().get(0);
    assertEquals(3, clip.events().size());
    assertTrue(
        clip.events().stream()
            .allMatch(event -> event.code().equals(DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE)));
    assertEquals(
        3,
        result.diagnostics().warnings().stream()
            .filter(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.ANIM_EVENT_IGNORED_BY_SCOPE))
            .count());
  }

  private static Path animation(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-animation-", ".animation.json");
    Files.writeString(path, json);
    return path;
  }
}
