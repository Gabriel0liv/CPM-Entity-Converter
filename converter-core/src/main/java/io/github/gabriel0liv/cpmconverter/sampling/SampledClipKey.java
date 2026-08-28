package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.ClipId;

public record SampledClipKey(ClipId clipId, int requestedFps, TimelineKind timelineKind) {
  public SampledClipKey {
    if (clipId == null || timelineKind == null) throw new IllegalArgumentException("sampled clip key");
    if (requestedFps < 1 || requestedFps > 240) throw new IllegalArgumentException("fps");
  }
}
