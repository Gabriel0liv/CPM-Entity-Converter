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
  void recognizesEveryGecko449BuiltInEasingName() throws Exception {
    String[][] easings = {
      {"linear", "LINEAR"},
      {"none", "LINEAR"},
      {"step", "STEP"},
      {"easeinsine", "EASE_IN_SINE"},
      {"easeoutsine", "EASE_OUT_SINE"},
      {"easeinoutsine", "EASE_IN_OUT_SINE"},
      {"easeinquad", "EASE_IN_QUAD"},
      {"easeoutquad", "EASE_OUT_QUAD"},
      {"easeinoutquad", "EASE_IN_OUT_QUAD"},
      {"easeincubic", "EASE_IN_CUBIC"},
      {"easeoutcubic", "EASE_OUT_CUBIC"},
      {"easeinoutcubic", "EASE_IN_OUT_CUBIC"},
      {"easeinquart", "EASE_IN_QUART"},
      {"easeoutquart", "EASE_OUT_QUART"},
      {"easeinoutquart", "EASE_IN_OUT_QUART"},
      {"easeinquint", "EASE_IN_QUINT"},
      {"easeoutquint", "EASE_OUT_QUINT"},
      {"easeinoutquint", "EASE_IN_OUT_QUINT"},
      {"easeinexpo", "EASE_IN_EXPO"},
      {"easeoutexpo", "EASE_OUT_EXPO"},
      {"easeinoutexpo", "EASE_IN_OUT_EXPO"},
      {"easeincirc", "EASE_IN_CIRC"},
      {"easeoutcirc", "EASE_OUT_CIRC"},
      {"easeinoutcirc", "EASE_IN_OUT_CIRC"},
      {"easeinback", "EASE_IN_BACK"},
      {"easeoutback", "EASE_OUT_BACK"},
      {"easeinoutback", "EASE_IN_OUT_BACK"},
      {"easeinelastic", "EASE_IN_ELASTIC"},
      {"easeoutelastic", "EASE_OUT_ELASTIC"},
      {"easeinoutelastic", "EASE_IN_OUT_ELASTIC"},
      {"easeinbounce", "EASE_IN_BOUNCE"},
      {"easeoutbounce", "EASE_OUT_BOUNCE"},
      {"easeinoutbounce", "EASE_IN_OUT_BOUNCE"},
      {"catmullrom", "CATMULLROM"}
    };

    for (String[] easing : easings) {
      var result =
          parse(
              """
              {
                "animations":{
                  "ease":{
                    "animation_length":1.0,
                    "bones":{"body":{"rotation":{
                      "0.0":[0,0,0],
                      "1.0":{"easing":"%s","vector":[1,2,3]}
                    }}}
                  }
                }
              }
              """
                  .formatted(easing[0]));

      assertTrue(result.success(), () -> easing[0] + ": " + result.diagnostics().all());
      assertEquals(
          easing[1],
          result
              .value()
              .get(0)
              .tracks()
              .get(0)
              .rotation()
              .keyframes()
              .get(0)
              .interpolationAfter()
              .name(),
          easing[0]);
    }
  }

  @Test
  void discardsBedrockPrePostEasingLikeGecko449() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "prepost":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":{
                    "0.0":[0,0,0],
                    "1.0":{
                      "pre":[10,0,0],
                      "post":[20,0,0],
                      "easing":"easeinsine",
                      "easingArgs":[4]
                    }
                  }}}
                }
              }
            }
            """);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    var frames = result.value().get(0).tracks().get(0).rotation().keyframes();
    assertEquals("LINEAR", frames.get(0).interpolationAfter().name());
    assertTrue(frames.get(0).easingArgsAfter().isEmpty());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals("ANIM_PRE_POST_COLLAPSED_449")));
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
  void evaluatesNonTrivialConstantMolangArithmeticOffline() throws Exception {
    var result =
        parse(
            """
            {
              "animations":{
                "constant":{
                  "animation_length":1.0,
                  "bones":{"body":{"rotation":[
                    "return -(2 + 3) * 1e1 / 5",
                    "return 7 % 4",
                    "return +.5"
                  ]}}
                }
              }
            }
            """);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(
        new Vec3d(-10, 3, 0.5),
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
