package io.github.gabriel0liv.cpmconverter.ir;

public record ClipId(String value) {
  public ClipId {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("empty clip id");
  }
}
