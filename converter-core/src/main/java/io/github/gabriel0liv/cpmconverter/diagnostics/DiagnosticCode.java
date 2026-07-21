package io.github.gabriel0liv.cpmconverter.diagnostics;

/** Stable diagnostic identifier. Production callers should use {@link DiagnosticCodes}. */
public record DiagnosticCode(String value) {
  public DiagnosticCode {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("diagnostic code is empty");
    }
  }

  /**
   * Creates a code at a catalog boundary. This is the preferred factory for adapters that receive a
   * textual code from a validated external document.
   *
   * @throws IllegalArgumentException when the value is not in the normative catalog
   */
  public static DiagnosticCode fromCatalog(String value) {
    if (!DiagnosticCodes.all().contains(value)) {
      throw new IllegalArgumentException("unknown diagnostic code: " + value);
    }
    return new DiagnosticCode(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
