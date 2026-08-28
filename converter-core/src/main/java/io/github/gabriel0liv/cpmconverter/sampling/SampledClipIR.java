package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import java.util.List;

public record SampledClipIR(
    SampledClipKey key,
    double sourceDurationSeconds,
    PlaybackMode sourcePlayback,
    SamplingMetadataIR metadata,
    List<SampledFrameIR> frames) {
  public SampledClipIR {
    if (key == null
        || !Double.isFinite(sourceDurationSeconds)
        || sourceDurationSeconds <= 0
        || sourcePlayback == null
        || metadata == null
        || frames == null) {
      throw new IllegalArgumentException("sampled clip");
    }
    frames = List.copyOf(frames);
  }
}
