package io.github.gabriel0liv.cpmconverter.ir;

public record CubeId(String value) {
  public CubeId {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("empty cube id");
  }
}
