package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;

public record BoneIR(
    BoneId id,
    String name,
    BoneId parent,
    List<BoneId> children,
    Transform bind,
    List<CubeIR> cubes,
    String provenance) {
  public BoneIR {
    if (id == null || name == null || bind == null) throw new IllegalArgumentException("bone");
    children = List.copyOf(children == null ? List.of() : children);
    cubes = List.copyOf(cubes == null ? List.of() : cubes);
    if (provenance == null) throw new IllegalArgumentException("provenance");
  }

  public BoneIR(
      BoneId id,
      String name,
      BoneId parent,
      List<BoneId> children,
      Transform bind,
      List<CubeIR> cubes) {
    this(id, name, parent, children, bind, cubes, "");
  }
}
