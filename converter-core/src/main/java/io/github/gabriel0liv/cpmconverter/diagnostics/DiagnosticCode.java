package io.github.gabriel0liv.cpmconverter.diagnostics;

/** Stable diagnostic identifier. Production callers should use {@link DiagnosticCodes}. */
public final class DiagnosticCode {
  private final String value;

  DiagnosticCode(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("diagnostic code is empty");
    }
    this.value = value;
  }

  public String value() {
    return value;
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
  public boolean equals(Object other) {
    return other instanceof DiagnosticCode that && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
