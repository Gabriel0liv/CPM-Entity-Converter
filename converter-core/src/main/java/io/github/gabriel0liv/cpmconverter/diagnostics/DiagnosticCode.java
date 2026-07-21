package io.github.gabriel0liv.cpmconverter.diagnostics;

/** Stable diagnostic identifier. Production callers should use {@link DiagnosticCodes}. */
public record DiagnosticCode(String value) {
  public DiagnosticCode {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("diagnostic code is empty");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
