package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedAnimationV1(String name, List<CpmPersistedFrameV1> frames) { public CpmPersistedAnimationV1 { frames=List.copyOf(frames); } }
