package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeckoEasingEvaluatorTest {
  private final GeckoEasingEvaluator evaluator = new GeckoEasingEvaluator();

  @Test
  void representativeBuiltins() {
    assertEquals(.5, evaluator.evaluateProgress(EasingIR.linear(), .5), 1e-9);
    assertEquals(
        .0625,
        evaluator.evaluateProgress(new EasingIR(EasingKindIR.EASE_IN_QUINT, List.of()), .5),
        1e-9);
    assertEquals(
        .96875,
        evaluator.evaluateProgress(new EasingIR(EasingKindIR.EASE_OUT_QUINT, List.of()), .5),
        1e-9);
    assertEquals(
        1, evaluator.evaluateProgress(new EasingIR(EasingKindIR.EASE_IN_SINE, List.of()), 1), 0);
  }

  @Test
  void stepAndProgressValidation() {
    assertEquals(
        0, evaluator.evaluateProgress(new EasingIR(EasingKindIR.STEP, List.of()), .5), 1e-9);
    assertEquals(1, evaluator.evaluateProgress(new EasingIR(EasingKindIR.STEP, List.of(4d)), 1), 0);
    assertThrows(
        IllegalArgumentException.class, () -> evaluator.evaluateProgress(EasingIR.linear(), 1.1));
  }
}
