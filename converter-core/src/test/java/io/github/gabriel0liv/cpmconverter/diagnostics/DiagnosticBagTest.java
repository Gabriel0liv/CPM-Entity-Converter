package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DiagnosticBagTest {
  @Test
  void completeOrderingIsDeterministic() {
    DiagnosticBag bag =
        new DiagnosticBag()
            .add(
                new Diagnostic(
                    Severity.INFO,
                    new DiagnosticCode("C"),
                    null,
                    "i",
                    null,
                    "b",
                    "a",
                    new java.util.TreeMap<>(java.util.Map.of("z", "1"))))
            .add(
                new Diagnostic(
                    Severity.ERROR,
                    new DiagnosticCode("A"),
                    null,
                    "e",
                    null,
                    "b",
                    "a",
                    new java.util.TreeMap<>()))
            .add(
                new Diagnostic(
                    Severity.WARNING,
                    new DiagnosticCode("B"),
                    null,
                    "w",
                    null,
                    "b",
                    "a",
                    new java.util.TreeMap<>()));
    assertEquals(Severity.ERROR, bag.all().get(0).severity());
    assertEquals(Severity.WARNING, bag.all().get(1).severity());
    assertEquals(Severity.INFO, bag.all().get(2).severity());
  }

  @Test
  void mergePreservesAllSeverities() {
    DiagnosticBag first =
        new DiagnosticBag().add(Diagnostic.of(Severity.INFO, new DiagnosticCode("A"), "a"));
    DiagnosticBag second =
        new DiagnosticBag().add(Diagnostic.of(Severity.WARNING, new DiagnosticCode("B"), "b"));
    assertEquals(2, first.addAll(second).all().size());
  }
}
