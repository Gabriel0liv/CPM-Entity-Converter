package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ResultTest {
  @Test
  void successAndWarning() {
    Diagnostic warning =
        Diagnostic.of(Severity.WARNING, new DiagnosticCode("TEST_WARNING"), "warn");
    Result<String> result = Result.success("value", new DiagnosticBag().add(warning));
    assertTrue(result.success());
    assertEquals("value", result.value());
    assertEquals(1, result.diagnostics().all().size());
  }

  @Test
  void failedMapAndFlatMapAreNotExecuted() {
    Result<String> result =
        Result.failure(Diagnostic.of(Severity.ERROR, new DiagnosticCode("TEST_ERROR"), "bad"));
    AtomicBoolean called = new AtomicBoolean();
    assertFalse(
        result
            .map(
                value -> {
                  called.set(true);
                  return value.length();
                })
            .success());
    assertFalse(
        result
            .flatMap(
                value -> {
                  called.set(true);
                  return Result.success(value.length());
                })
            .success());
    assertFalse(called.get());
  }

  @Test
  void incoherentFactoriesAreRejected() {
    Diagnostic info = Diagnostic.of(Severity.INFO, new DiagnosticCode("TEST_INFO"), "info");
    assertThrows(
        IllegalArgumentException.class, () -> Result.failure(new DiagnosticBag().add(info)));
    assertThrows(IllegalArgumentException.class, () -> Result.success(null));
  }
}
