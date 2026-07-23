package io.github.gabriel0liv.cpmconverter.projection;

public record CpmNodeKey(String value) {
  public CpmNodeKey {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("key");
  }
}
