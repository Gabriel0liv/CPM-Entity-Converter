package io.github.gabriel0liv.cpmconverter.projection;

import java.util.*;

public record CpmPerFaceUvV1(Map<CpmCubeFace, CpmFaceUvV1> faces) implements CpmUvV1 {
  public CpmPerFaceUvV1 {
    if (faces == null || faces.isEmpty() || faces.containsKey(null) || faces.containsValue(null))
      throw new IllegalArgumentException("faces");
    EnumMap<CpmCubeFace, CpmFaceUvV1> copy = new EnumMap<>(CpmCubeFace.class);
    for (CpmCubeFace f : CpmCubeFace.values()) if (faces.containsKey(f)) copy.put(f, faces.get(f));
    faces = Collections.unmodifiableMap(copy);
  }
}
