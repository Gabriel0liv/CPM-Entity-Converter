package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class DiagnosticBagTest {
  @Test
  void completeOrderingIsDeterministic() {
    DiagnosticBag first =
        new DiagnosticBag(
            List.of(
                diagnostic("z.json", 2, 1, "/b", "B"),
                diagnostic("a.json", 9, 1, "/z", "A"),
                diagnostic("a.json", 3, 4, "/a", "C")));
    DiagnosticBag second =
        new DiagnosticBag(
            List.of(
                diagnostic("a.json", 3, 4, "/a", "C"),
                diagnostic("z.json", 2, 1, "/b", "B"),
                diagnostic("a.json", 9, 1, "/z", "A")));
    assertEquals(first.all(), second.all());
    assertEquals("C", first.all().get(0).code().value());
    assertEquals("A", first.all().get(1).code().value());
    assertEquals("B", first.all().get(2).code().value());
  }

  private static Diagnostic diagnostic(
      String source, int line, int column, String pointer, String code) {
    return new Diagnostic(
        Severity.ERROR,
        new DiagnosticCode(code),
        new SourceLocation(new SourcePath(source), line, column, pointer, null),
        "message",
        null,
        "bone",
        "clip",
        new TreeMap<>(Map.of("b", "2", "a", "1")));
  }

  @Test
  void severityIsThePrimaryOrderingKey() {
    DiagnosticBag bag =
        new DiagnosticBag(
            List.of(
                Diagnostic.of(Severity.INFO, new DiagnosticCode("I"), "info"),
                Diagnostic.of(Severity.ERROR, new DiagnosticCode("E"), "error"),
                Diagnostic.of(Severity.WARNING, new DiagnosticCode("W"), "warning")));
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

  @Test
  void collectionIsImmutableAndRejectsNull() {
    Diagnostic diagnostic = Diagnostic.of(Severity.INFO, new DiagnosticCode("A"), "a");
    DiagnosticBag bag = new DiagnosticBag(List.of(diagnostic));
    assertThrows(UnsupportedOperationException.class, () -> bag.all().add(diagnostic));
    assertThrows(NullPointerException.class, () -> new DiagnosticBag(List.of((Diagnostic) null)));
  }
}
