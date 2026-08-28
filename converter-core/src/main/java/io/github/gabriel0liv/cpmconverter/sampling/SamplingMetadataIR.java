package io.github.gabriel0liv.cpmconverter.sampling;

public record SamplingMetadataIR(
    int requestedFps,
    int frameCount,
    double frameDensity,
    double effectiveIntervalRate,
    double frameInterval,
    double maxTemporalGridError) {
  public SamplingMetadataIR {
    if (requestedFps < 1 || requestedFps > 240) throw new IllegalArgumentException("fps");
    if (frameCount < 1) throw new IllegalArgumentException("frame count");
    if (!Double.isFinite(frameDensity) || frameDensity <= 0) {
      throw new IllegalArgumentException("frame density");
    }
    if (!finiteNonNegative(effectiveIntervalRate)
        || !finiteNonNegative(frameInterval)
        || !finiteNonNegative(maxTemporalGridError)) {
      throw new IllegalArgumentException("sampling metadata");
    }
  }

  private static boolean finiteNonNegative(double value) {
    return Double.isFinite(value) && value >= 0;
  }
}
