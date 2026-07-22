package io.github.gabriel0liv.cpmconverter.ir;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record PerFaceUvIR(Map<CubeFaceIR, FaceUvIR> faces) implements UvIR {
  public PerFaceUvIR {
    var copy = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
    if (faces != null) copy.putAll(faces);
    if (faces == null || faces.isEmpty()) throw new IllegalArgumentException("faces required");
    if (copy.size() != faces.size() || copy.containsKey(null) || copy.containsValue(null)) {
      throw new IllegalArgumentException("faces must not contain nulls");
    }
    faces = Collections.unmodifiableMap(copy);
  }
}
