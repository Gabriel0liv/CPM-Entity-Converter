package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import java.util.*;

public record AnimationClipIR(
    ClipId id,
    double duration,
    PlaybackMode playback,
    String customLoop,
    List<BoneTrackIR> tracks,
    List<UnsupportedEventIR> events,
    SourceLocation source) {
  public AnimationClipIR {
    if (id == null
        || !Double.isFinite(duration)
        || duration <= 0
        || playback == null
        || source == null) throw new IllegalArgumentException("clip");
    tracks = List.copyOf(tracks == null ? List.of() : tracks);
    events = List.copyOf(events == null ? List.of() : events);
    if (playback == PlaybackMode.CUSTOM && (customLoop == null || customLoop.isBlank()))
      throw new IllegalArgumentException("custom loop");
  }
}
