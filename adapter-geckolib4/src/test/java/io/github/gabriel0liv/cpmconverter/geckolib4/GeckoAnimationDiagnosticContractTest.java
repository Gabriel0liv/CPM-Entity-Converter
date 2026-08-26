package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoAnimationDiagnosticContractTest {
  @Test
  void unknownBoneUsesNormativeDiagnosticCode() throws Exception {
    var geometry = fixtureGeometry();
    Path animation =
        animation(
            """
            {
              "animations":{
                "bad_bone":{
                  "animation_length":1.0,
                  "bones":{"does_not_exist":{"rotation":[0,0,0]}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry);

    assertFalse(result.success());
    assertEquals("ANIM_BONE_NOT_FOUND", result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void nonPositiveDurationUsesNormativeDiagnosticCode() throws Exception {
    var geometry = fixtureGeometry();
    Path animation =
        animation(
            """
            {
              "animations":{
                "zero":{
                  "animation_length":0,
                  "bones":{"body":{"rotation":[0,0,0]}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry);

    assertFalse(result.success());
    assertEquals("ANIM_ZERO_DURATION_INVALID", result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void malformedAnimationValueUsesInputParseError() throws Exception {
    var geometry = fixtureGeometry();
    Path animation =
        animation(
            """
            {
              "animations":{
                "bad_vector":{
                  "animation_length":1.0,
                  "bones":{"body":{"position":[1,2]}}
                }
              }
            }
            """);

    var result = new GeckoAnimationParser().parse(animation, geometry);

    assertFalse(result.success());
    assertEquals("INPUT_PARSE_ERROR", result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void unboundedImplicitDurationUsesNormativeDiagnosticCode() throws Exception {
    var geometry = fixtureGeometry();
    Path animation = animation("{\"animations\":{\"empty\":{\"bones\":{}}}}");

    var result = new GeckoAnimationParser().parse(animation, geometry);

    assertFalse(result.success());
    assertEquals(
        "ANIM_IMPLICIT_LENGTH_UNBOUNDED", result.diagnostics().errors().get(0).code().value());
  }

  private static io.github.gabriel0liv.cpmconverter.ir.ModelIR fixtureGeometry() {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var geometry = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(geometry.success(), () -> geometry.diagnostics().all().toString());
    return geometry.value();
  }

  private static Path animation(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-animation-diagnostic-", ".animation.json");
    Files.writeString(path, json);
    return path;
  }
}
