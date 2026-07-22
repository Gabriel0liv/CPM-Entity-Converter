package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConstantMolangEvaluatorTest {
  private final ConstantMolangEvaluator evaluator = new ConstantMolangEvaluator();

  private SourceLocation source() {
    return SourceLocation.of(new SourcePath("test.animation.json"));
  }

  @Test
  void evaluatesConstantsAndFunctions() {
    assertEquals(5, evaluator.evaluate("return 2 + 3", source(), Map.of()).value(), 1e-9);
    assertEquals(8, evaluator.evaluate("math.pow(2,3)", source(), Map.of()).value(), 1e-9);
    assertEquals(1, evaluator.evaluate("math.sin(90)", source(), Map.of()).value(), 1e-9);
    assertEquals(Math.PI, evaluator.evaluate("math.pi", source(), Map.of()).value(), 1e-9);
  }

  @Test
  void distinguishesDynamicAndInvalid() {
    var dynamic = evaluator.evaluate("query.anim_time", source(), Map.of());
    assertFalse(dynamic.success());
    assertEquals(
        DiagnosticCodes.ANIM_DYNAMIC_MOLANG_UNSUPPORTED,
        dynamic.diagnostics().all().get(0).code().value());
    var invalid = evaluator.evaluate("2 +", source(), Map.of());
    assertFalse(invalid.success());
    assertEquals(
        DiagnosticCodes.ANIM_MOLANG_PARSE_ERROR, invalid.diagnostics().all().get(0).code().value());
  }
}
