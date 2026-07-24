package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedAnimationV1(String entryName, String logicalName, CpmPersistedAnimationKind kind,
    boolean additive, int durationMillis, int priority, boolean loop, CpmPersistedInterpolator interpolator,
    List<CpmPersistedFrameV1> frames) {
  public CpmPersistedAnimationV1 { frames=List.copyOf(frames); }
}
