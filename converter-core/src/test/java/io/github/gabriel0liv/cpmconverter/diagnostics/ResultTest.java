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
  void mapPreservesWarningsWhileTransformingValue() {
    Diagnostic warning =
        Diagnostic.of(Severity.WARNING, new DiagnosticCode("TEST_WARNING"), "warn");
    Result<Integer> mapped =
        Result.success("value", new DiagnosticBag().add(warning)).map(String::length);

    assertTrue(mapped.success());
    assertEquals(5, mapped.value());
    assertEquals(1, mapped.diagnostics().warnings().size());
    assertEquals("TEST_WARNING", mapped.diagnostics().warnings().get(0).code().value());
  }

  @Test
  void flatMapMergesDiagnosticsFromBothStages() {
    Diagnostic first =
        Diagnostic.of(Severity.WARNING, new DiagnosticCode("FIRST_WARNING"), "first");
    Diagnostic second = Diagnostic.of(Severity.INFO, new DiagnosticCode("SECOND_INFO"), "second");

    Result<Integer> result =
        Result.success("value", new DiagnosticBag().add(first))
            .flatMap(value -> Result.success(value.length(), new DiagnosticBag().add(second)));

    assertTrue(result.success());
    assertEquals(5, result.value());
    assertEquals(2, result.diagnostics().all().size());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals("FIRST_WARNING")));
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals("SECOND_INFO")));
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
