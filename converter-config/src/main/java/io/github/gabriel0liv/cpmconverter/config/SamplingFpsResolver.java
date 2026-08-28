package io.github.gabriel0liv.cpmconverter.config;

public final class SamplingFpsResolver {
  private SamplingFpsResolver() {}

  public static int resolve(CompiledStateMapping state, CompiledSamplingPolicy global) {
    if (state != null && state.requestedFps() != null) return state.requestedFps();
    if (global != null) return global.requestedFps();
    return 20;
  }
}
