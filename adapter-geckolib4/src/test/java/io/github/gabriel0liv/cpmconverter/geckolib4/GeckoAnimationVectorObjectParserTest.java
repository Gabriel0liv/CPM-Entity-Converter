package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoAnimationVectorObjectParserTest {
  @Test
  void parsesTimestampVectorObjectsAcrossTransformChannels() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());

    Path animation = Files.createTempFile("cpm-converter-vector-object-", ".animation.json");
    Files.writeString(
        animation,
        """
        {
          "animations":{
            "vector_object":{
              "animation_length":1.0,
              "bones":{"body":{
                "position":{"0.0":{"vector":[1,2,3]}},
                "rotation":{"0.0":{"vector":[4,5,6]}},
                "scale":{"0.0":{"vector":[7,8,9]}}
              }}
            }
          }
        }
        """);

    var result = new GeckoAnimationParser().parse(animation, geometry.value());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var track = result.value().get(0).tracks().get(0);
    assertEquals(new Vec3d(-1, -2, 3), track.position().keyframes().get(0).incomingValue());
    assertEquals(new Vec3d(4, 5, 6), track.rotation().keyframes().get(0).incomingValue());
    assertEquals(new Vec3d(7, 8, 9), track.scale().keyframes().get(0).incomingValue());
  }
}
