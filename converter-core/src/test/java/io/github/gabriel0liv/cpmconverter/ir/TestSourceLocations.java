package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;

/** Deterministic source locations used only by ModelIR tests. */
final class TestSourceLocations {
  private TestSourceLocations() {}

  static SourceLocation at(String pointer) {
    return new SourceLocation(
        new SourcePath("fixtures/test.animation.json"), null, null, pointer, null);
  }

  static SourceLocation animation(String pointer) {
    return new SourceLocation(
        new SourcePath("fixtures/test.animation.json"), null, null, pointer, null);
  }

  static SourceLocation geometry(String pointer) {
    return new SourceLocation(new SourcePath("fixtures/test.geo.json"), null, null, pointer, null);
  }
}
