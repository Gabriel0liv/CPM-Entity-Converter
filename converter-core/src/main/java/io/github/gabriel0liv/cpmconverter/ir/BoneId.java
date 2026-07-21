package io.github.gabriel0liv.cpmconverter.ir;

public record BoneId(String value) {
  public BoneId {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("empty bone id");
  }
}
