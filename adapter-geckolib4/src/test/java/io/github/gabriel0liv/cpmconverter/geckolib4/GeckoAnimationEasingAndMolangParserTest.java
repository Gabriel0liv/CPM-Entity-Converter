package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeckoAnimationEasingAndMolangParserTest {
  @Test
  void shiftsTargetKeyframeEasingOntoPreviousIrSegment() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "ease":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":{
                    "0.0":[0,0,0],
                    "1.0":{"easing":"easeinsine","vector":[10,20,30]}
                  }}}
                }
              }
            }
            """);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frames = result.value().get(0).tracks().get(0).rotation().keyframes();
    assertEquals(2, frames.size());
    assertEquals("EASE_IN_SINE", frames.get(0).interpolationAfter().name());
    assertEquals("LINEAR", frames.get(1).interpolationAfter().name());
  }

  @Test
  void preservesAllEasingArgsOnTheSegmentEvenThoughGeckoUsesOnlyTheFirst() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "ease":{
                  "animation_length":1.0,
                  "bones":{"body":{"position":{
                    "0.0":[0,0,0],
                    "1.0":{"easing":"easeinback","easingArgs":[1.2,0.35],"vector":[10,0,0]}
                  }}}
                }
              }
            }
            """);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frames = result.value().get(0).tracks().get(0).position().keyframes();
    assertEquals("EASE_IN_BACK", frames.get(0).interpolation().name());
    assertEquals(List.of(1.2, 0.35), frames.get(0).easingArgs());
    assertTrue(frames.get(1).easingArgs().isEmpty());
  }

  @Test
  void rejectsUnknownPotentiallyRuntimeRegisteredCustomEasing() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "custom":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":{
                    "0.0":[0,0,0],
                    "1.0":{"easing":"my_mod_curve","vector":[1,2,3]}
                  }}}
                }
              }
            }
            """);

    assertFalse(result.success());
    assertEquals(
        "ANIM_CUSTOM_EASING_UNSUPPORTED", result.diagnostics().errors().get(0).code().value());
  }

  @Test
  void evaluatesConstantMolangComponentsOffline() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "constant":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":["return 2 + 3","return 4","return 5"]}}
                }
              }
            }
            """);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(
        new Vec3d(5, 4, 5),
        result.value().get(0).tracks().get(0).rotation().keyframes().get(0).incomingValue());
  }

  @Test
  void rejectsRuntimeDependentMolangInsteadOfGuessingAValue() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "dynamic":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":["query.anim_time * 40",0,0]}}
                }
              }
            }
            """);

    assertFalse(result.success());
    assertEquals(
        "ANIM_DYNAMIC_MOLANG_UNSUPPORTED", result.diagnostics().errors().get(0).code().value());
  }

  private static io.github.gabriel0liv.cpmconverter.diagnostics.Result<
          List<io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR>>
      parse(String json) throws Exception {
    ModelIR geometry = geometry();
    Path animation = Files.createTempFile("cpm-converter-t203-", ".animation.json");
    Files.writeString(animation, json);
    return new GeckoAnimationParser().parse(animation, geometry);
  }

  private static ModelIR geometry() {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    var result = new GeckoGeometryParser().parse(fixture.resolve("geometry.geo.json"));
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    return result.value();
  }
}
