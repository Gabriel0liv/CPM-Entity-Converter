package io.github.gabriel0liv.cpmconverter.sampling;

import java.util.ArrayList;
import java.util.List;

public final class TimelineGrid {
  private TimelineGrid() {}

  public static Result build(double durationSeconds, int requestedFps, TimelineKind kind) {
    if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
      throw new IllegalArgumentException("duration");
    }
    if (requestedFps < 1 || requestedFps > 240) throw new IllegalArgumentException("fps");
    if (kind == null) throw new IllegalArgumentException("timeline kind");

    double rawCount = durationSeconds * requestedFps;
    if (!Double.isFinite(rawCount)) throw new IllegalArgumentException("frame count");
    long rounded = Math.round(rawCount);
    if (rounded > Integer.MAX_VALUE) throw new IllegalArgumentException("frame count");
    int frameCount = Math.max(1, (int) rounded);

    List<Double> times = new ArrayList<>(frameCount);
    double frameInterval;
    double effectiveIntervalRate;
    if (kind == TimelineKind.LOOP) {
      for (int i = 0; i < frameCount; i++) {
        times.add(i * durationSeconds / frameCount);
      }
      frameInterval = durationSeconds / frameCount;
      effectiveIntervalRate = frameCount / durationSeconds;
    } else if (frameCount == 1) {
      times.add(0.0);
      frameInterval = 0.0;
      effectiveIntervalRate = 0.0;
    } else {
      for (int i = 0; i < frameCount; i++) {
        times.add(i * durationSeconds / (frameCount - 1));
      }
      frameInterval = durationSeconds / (frameCount - 1);
      effectiveIntervalRate = (frameCount - 1) / durationSeconds;
    }

    double frameDensity = frameCount / durationSeconds;
    double maxTemporalGridError = 0.0;
    for (int i = 0; i < times.size(); i++) {
      maxTemporalGridError =
          Math.max(maxTemporalGridError, Math.abs(times.get(i) - i / (double) requestedFps));
    }

    var metadata =
        new SamplingMetadataIR(
            requestedFps,
            frameCount,
            frameDensity,
            effectiveIntervalRate,
            frameInterval,
            maxTemporalGridError);
    return new Result(times, metadata);
  }

  public record Result(List<Double> times, SamplingMetadataIR metadata) {
    public Result {
      if (times == null || metadata == null) throw new IllegalArgumentException("timeline result");
      times = List.copyOf(times);
    }
  }
}
