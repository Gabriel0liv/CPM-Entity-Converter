package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
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
}
