package io.github.gabriel0liv.cpmconverter.diagnostics;

import java.util.Optional;

public record SourceLocation(
    SourcePath source, Integer line, Integer column, String jsonPointer, Long byteOffset) {
  public SourceLocation {
    if (source == null) throw new IllegalArgumentException("source required");
    if (line != null && line < 1) throw new IllegalArgumentException("line");
    if (column != null && column < 1) throw new IllegalArgumentException("column");
    if (jsonPointer != null && jsonPointer.isBlank()) jsonPointer = null;
    if (byteOffset != null && byteOffset < 0) throw new IllegalArgumentException("byteOffset");
  }

  public static SourceLocation of(SourcePath p) {
    return new SourceLocation(p, null, null, null, null);
  }

  public Optional<Integer> lineOpt() {
    return Optional.ofNullable(line);
  }
}
