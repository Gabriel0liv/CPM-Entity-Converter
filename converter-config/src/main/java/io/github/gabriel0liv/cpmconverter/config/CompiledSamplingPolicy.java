package io.github.gabriel0liv.cpmconverter.config;

public record CompiledSamplingPolicy(int requestedFps) {
  public CompiledSamplingPolicy {
    if (requestedFps < 1 || requestedFps > 240) throw new IllegalArgumentException("fps");
  }
}
