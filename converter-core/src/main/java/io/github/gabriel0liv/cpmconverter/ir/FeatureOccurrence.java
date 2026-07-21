package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;

public record FeatureOccurrence(String feature, SourceLocation source) {
  public FeatureOccurrence {
    if (feature == null || feature.isBlank()) throw new IllegalArgumentException("feature");
  }

  @Deprecated
  public FeatureOccurrence(String feature, String source) {
    this(
        feature,
        SourceLocation.of(
            new SourcePath(source == null || source.isBlank() ? "legacy/model" : source)));
  }
}
