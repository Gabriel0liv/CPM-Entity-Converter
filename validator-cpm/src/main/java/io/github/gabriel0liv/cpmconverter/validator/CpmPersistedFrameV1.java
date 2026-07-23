package io.github.gabriel0liv.cpmconverter.validator;
import java.util.List;
public record CpmPersistedFrameV1(double time, List<CpmPersistedFrameComponentV1> components) { public CpmPersistedFrameV1 { components=List.copyOf(components); } }
