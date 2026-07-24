package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedFrameV1(int index, List<CpmPersistedFrameComponentV1> components, String pointer) {
  public CpmPersistedFrameV1 { components=List.copyOf(components); }
}
