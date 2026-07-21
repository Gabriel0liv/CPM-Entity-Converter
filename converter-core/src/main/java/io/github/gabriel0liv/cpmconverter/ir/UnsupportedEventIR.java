package io.github.gabriel0liv.cpmconverter.ir;

public record UnsupportedEventIR(String code, String message, String source) {
  public UnsupportedEventIR {
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code");
  }
}
