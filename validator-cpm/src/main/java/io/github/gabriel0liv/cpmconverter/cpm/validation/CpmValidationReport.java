package io.github.gabriel0liv.cpmconverter.cpm.validation;

/** Deterministic summary of the CPM V1 structures inspected by the validator. */
public record CpmValidationReport(
    int entryCount, int elementCount, int animationCount, int storeIdCount) {
  public CpmValidationReport {
    if (entryCount < 0 || elementCount < 0 || animationCount < 0 || storeIdCount < 0) {
      throw new IllegalArgumentException("validation counts must be non-negative");
    }
  }
}
