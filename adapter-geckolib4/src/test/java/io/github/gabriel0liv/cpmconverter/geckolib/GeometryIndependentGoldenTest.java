package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GeometryIndependentGoldenTest {
  private static final double EPSILON = 1e-9;

  @Test
  void fixtureTranslationsMatchIndependentCoordinateDerivation() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":["
            + "{\"name\":\"body\",\"pivot\":[0,0,0]},"
            + "{\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,8,0]},"
            + "{\"name\":\"left_arm\",\"parent\":\"body\",\"pivot\":[3,7,0]},"
            + "{\"name\":\"neck\",\"parent\":\"head\",\"pivot\":[0,8,0]}]}]}";
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("golden.geo.json"),
                GeometryParseRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals(-8.0, result.value().bones().get(1).bindLocal().translation().y(), EPSILON);
    assertEquals(-3.0, result.value().bones().get(2).bindLocal().translation().x(), EPSILON);
    assertEquals(-7.0, result.value().bones().get(2).bindLocal().translation().y(), EPSILON);
  }

  @Test
  void deepFixtureAccessoryTranslationMatchesIndependentDerivation() {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":["
            + "{\"name\":\"body\",\"pivot\":[0,0,0]},"
            + "{\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,14,1]},"
            + "{\"name\":\"accessory\",\"parent\":\"head\",\"pivot\":[2,15,0]}]}]}";
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("deep.geo.json"),
                GeometryParseRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    var translation = result.value().bones().get(2).bindLocal().translation();
    assertEquals(-2.0, translation.x(), EPSILON);
    assertEquals(-1.0, translation.y(), EPSILON);
    assertEquals(-1.0, translation.z(), EPSILON);
  }

  @Test
  void sourceNinetyDegreeRotationsMatchManualQuaternions() {
    assertRotation("[90,0,0]", 0.7071067811865476, -0.7071067811865475, 0, 0);
    assertRotation("[0,90,0]", 0.7071067811865476, 0, -0.7071067811865475, 0);
    assertRotation("[0,0,90]", 0.7071067811865476, 0, 0, 0.7071067811865475);
    assertRotation(
        "[12,0,27]",
        0.9670431762329943,
        -0.10164033350685966,
        -0.024401685140816008,
        0.23216652572691124);
  }

  private void assertRotation(String rotation, double w, double x, double y, double z) {
    String json =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\",\"rotation\":"
            + rotation
            + "}]}]}";
    var result =
        new GeckoGeometryParser()
            .parse(
                json.getBytes(StandardCharsets.UTF_8),
                new SourcePath("rotation.geo.json"),
                GeometryParseRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    var q = result.value().bones().get(0).bindLocal().rotation();
    assertEquals(w, q.w(), EPSILON);
    assertEquals(x, q.x(), EPSILON);
    assertEquals(y, q.y(), EPSILON);
    assertEquals(z, q.z(), EPSILON);
  }
}
