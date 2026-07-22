package io.github.gabriel0liv.cpmconverter.ir;

import java.util.EnumMap;
import java.util.Map;

public record PerFaceUvIR(Map<CubeFaceIR, FaceUvIR> faces) implements UvIR {
  public PerFaceUvIR {
    var copy = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
    if (faces != null) copy.putAll(faces);
    faces = Map.copyOf(copy);
  }
}
