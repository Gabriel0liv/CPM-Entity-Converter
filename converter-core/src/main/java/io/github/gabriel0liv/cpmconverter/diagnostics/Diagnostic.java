package io.github.gabriel0liv.cpmconverter.diagnostics;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public record Diagnostic(
    Severity severity,
    DiagnosticCode code,
    SourceLocation location,
    String message,
    String suggestion,
    String bone,
    String animation,
    NavigableMap<String, String> context) {
  public Diagnostic {
    Objects.requireNonNull(severity);
    Objects.requireNonNull(code);
    if (message == null || message.isBlank()) throw new IllegalArgumentException("message");
    context =
        Collections.unmodifiableNavigableMap(new TreeMap<>(context == null ? Map.of() : context));
  }

  /** Compatibility boundary for adapters; production code should use catalog symbols. */
  @Deprecated(forRemoval = false)
  public static Diagnostic of(Severity severity, String code, String message) {
    return of(severity, new DiagnosticCode(code), message);
  }

  public static Diagnostic of(Severity s, DiagnosticCode c, String m) {
    return new Diagnostic(s, c, null, m, null, null, null, new TreeMap<>());
  }

  /** Returns a stable, human-readable representation suitable for reports and tests. */
  public String canonicalForm() {
    StringBuilder result = new StringBuilder();
    result.append("severity=").append(severity);
    result.append(";code=").append(code.value());
    if (location != null) {
      result.append(";source=").append(location.source().value());
      result.append(";line=").append(location.line());
      result.append(";column=").append(location.column());
      result.append(";pointer=").append(location.jsonPointer());
      result.append(";offset=").append(location.byteOffset());
    }
    if (bone != null) result.append(";bone=").append(bone);
    if (animation != null) result.append(";animation=").append(animation);
    context.forEach(
        (key, value) -> result.append(";context.").append(key).append('=').append(value));
    result.append(";message=").append(message);
    if (suggestion != null) result.append(";suggestion=").append(suggestion);
    return result.toString();
  }
}
