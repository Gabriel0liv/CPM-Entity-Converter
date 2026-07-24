package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedAnimationV1(String entryName, String logicalName, CpmPersistedAnimationKind kind,
    boolean additive, int durationMillis, int priority, boolean loop, CpmPersistedInterpolator interpolator,
    List<CpmPersistedFrameV1> frames) {
  public CpmPersistedAnimationV1 { frames=List.copyOf(frames == null ? List.of() : frames); }
  public CpmPersistedAnimationV1(String name, List<CpmPersistedFrameV1> frames) { this(name,name,CpmPersistedAnimationKind.VANILLA,false,0,0,false,CpmPersistedInterpolator.NO_INTERPOLATE,frames); }
  public String name(){return logicalName;}
}
