package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedFrameV1(int index, List<CpmPersistedFrameComponentV1> components, String pointer) {
  public CpmPersistedFrameV1 { components=List.copyOf(components == null ? List.of() : components); }
  public CpmPersistedFrameV1(double ignored, List<CpmPersistedFrameComponentV1> components){this((int)ignored,components,null);}
}
