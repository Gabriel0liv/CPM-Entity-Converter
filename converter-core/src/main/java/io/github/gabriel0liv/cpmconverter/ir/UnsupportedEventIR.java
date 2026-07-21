package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;

public record UnsupportedEventIR(String code, String message, SourceLocation source) {
  public UnsupportedEventIR {
    if (code == null || code.isBlank()) throw new IllegalArgumentException("code");
  }

  @Deprecated
  public UnsupportedEventIR(String code, String message, String source) {
    this(
        code,
        message,
        SourceLocation.of(
            new SourcePath(source == null || source.isBlank() ? "legacy/animation" : source)));
  }
}
