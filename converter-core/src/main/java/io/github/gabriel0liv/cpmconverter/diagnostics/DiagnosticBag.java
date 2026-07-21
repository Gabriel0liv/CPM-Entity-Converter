package io.github.gabriel0liv.cpmconverter.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class DiagnosticBag {
  private static int severityRank(Severity severity) {
    return switch (severity) {
      case ERROR -> 0;
      case WARNING -> 1;
      case INFO -> 2;
    };
  }

  private final List<Diagnostic> values;

  public DiagnosticBag() {
    this(List.of());
  }

  public DiagnosticBag(Collection<Diagnostic> input) {
    ArrayList<Diagnostic> sorted = new ArrayList<>(input);
    sorted.sort(
        Comparator.comparingInt((Diagnostic d) -> severityRank(d.severity()))
            .thenComparing(d -> d.location() == null ? "" : d.location().source().value())
            .thenComparingInt(
                d ->
                    d.location() == null || d.location().line() == null
                        ? Integer.MAX_VALUE
                        : d.location().line())
            .thenComparingInt(
                d ->
                    d.location() == null || d.location().column() == null
                        ? Integer.MAX_VALUE
                        : d.location().column())
            .thenComparing(
                d ->
                    d.location() == null || d.location().jsonPointer() == null
                        ? ""
                        : d.location().jsonPointer())
            .thenComparing(d -> d.code().value())
            .thenComparing(d -> Objects.toString(d.bone(), ""))
            .thenComparing(d -> Objects.toString(d.animation(), ""))
            .thenComparing(d -> d.context().toString()));
    values = List.copyOf(sorted);
  }

  public DiagnosticBag add(Diagnostic value) {
    ArrayList<Diagnostic> next = new ArrayList<>(values);
    next.add(value);
    return new DiagnosticBag(next);
  }

  public DiagnosticBag addAll(DiagnosticBag other) {
    ArrayList<Diagnostic> next = new ArrayList<>(values);
    next.addAll(other.values);
    return new DiagnosticBag(next);
  }

  public List<Diagnostic> all() {
    return values;
  }

  public boolean hasErrors() {
    return values.stream().anyMatch(d -> d.severity() == Severity.ERROR);
  }

  public List<Diagnostic> warnings() {
    return values.stream().filter(d -> d.severity() == Severity.WARNING).toList();
  }

  public List<Diagnostic> errors() {
    return values.stream().filter(d -> d.severity() == Severity.ERROR).toList();
  }
}
