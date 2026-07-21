package io.github.gabriel0liv.cpmconverter.config;

/** A user-requested ignored feature with an explicit rationale. */
public record CompiledIgnoreRule(String feature, String reason) {
  public CompiledIgnoreRule {
    if (feature == null || feature.isBlank()) {
      throw new IllegalArgumentException("feature must not be blank");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
  }
}
