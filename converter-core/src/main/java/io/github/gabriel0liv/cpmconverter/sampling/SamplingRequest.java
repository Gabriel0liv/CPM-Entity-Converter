package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.ClipId;

public record SamplingRequest(ClipId clipId, int requestedFps, TimelineKind timelineKind) {
  public SamplingRequest {
    if (clipId == null || timelineKind == null)
      throw new IllegalArgumentException("sampling request");
    if (requestedFps < 1 || requestedFps > 240) throw new IllegalArgumentException("fps");
  }
}
