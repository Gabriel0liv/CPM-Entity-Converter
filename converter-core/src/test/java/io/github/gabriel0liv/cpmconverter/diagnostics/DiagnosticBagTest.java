package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class DiagnosticBagTest {
  @Test
  void completeOrderingIsDeterministic() {
    Diagnostic info = diagnostic(Severity.INFO, "Z_INFO", "b.json", 1, 1, "/z", Map.of());
    Diagnostic warning =
        diagnostic(Severity.WARNING, "A_WARNING", "a.json", 2, 1, "/a", Map.of());
    Diagnostic errorLaterLocation =
        diagnostic(Severity.ERROR, "B_ERROR", "a.json", 2, 1, "/b", Map.of());
    Diagnostic errorEarlierLocation =
        diagnostic(Severity.ERROR, "Z_ERROR", "a.json", 1, 9, "/z", Map.of());
    Diagnostic errorCodeTieBreak =
        diagnostic(Severity.ERROR, "A_ERROR", "a.json", 2, 1, "/b", Map.of());

    DiagnosticBag bag =
        new DiagnosticBag(
            List.of(info, warning, errorLaterLocation, errorEarlierLocation, errorCodeTieBreak));

    assertEquals(
        List.of("Z_ERROR", "A_ERROR", "B_ERROR", "A_WARNING", "Z_INFO"),
        bag.all().stream().map(diagnostic -> diagnostic.code().value()).toList());
  }

  @Test
  void contextIsNormalizedDeterministically() {
    TreeMap<String, String> reversed = new TreeMap<>((left, right) -> right.compareTo(left));
    reversed.put("z", "last");
    reversed.put("a", "first");

    Diagnostic diagnostic =
        diagnostic(Severity.WARNING, "CTX", "a.json", 1, 1, "/a", reversed);

    assertEquals(List.of("a", "z"), diagnostic.context().keySet().stream().toList());
  }

  @Test
  void mergePreservesAllSeverities() {
    DiagnosticBag first =
        new DiagnosticBag().add(Diagnostic.of(Severity.INFO, new DiagnosticCode("A"), "a"));
    DiagnosticBag second =
        new DiagnosticBag().add(Diagnostic.of(Severity.WARNING, new DiagnosticCode("B"), "b"));
    assertEquals(2, first.addAll(second).all().size());
  }

  private static Diagnostic diagnostic(
      Severity severity,
      String code,
      String source,
      int line,
      int column,
      String pointer,
      Map<String, String> context) {
    return new Diagnostic(
        severity,
        new DiagnosticCode(code),
        new SourceLocation(new SourcePath(source), line, column, pointer, null),
        code,
        null,
        null,
        null,
        new TreeMap<>(context));
  }
}
