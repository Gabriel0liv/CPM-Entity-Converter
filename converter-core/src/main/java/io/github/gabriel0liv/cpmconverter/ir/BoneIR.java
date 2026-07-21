package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;

public record BoneIR(
    BoneId id,
    String name,
    BoneId parent,
    List<BoneId> children,
    Transform bind,
    List<CubeIR> cubes,
    String provenance,
    SourceLocation sourceLocation) {
  public BoneIR {
    if (id == null || name == null || bind == null) throw new IllegalArgumentException("bone");
    children = List.copyOf(children == null ? List.of() : children);
    cubes = List.copyOf(cubes == null ? List.of() : cubes);
    if (provenance == null || sourceLocation == null)
      throw new IllegalArgumentException("provenance");
  }

  public BoneIR(
      BoneId id,
      String name,
      BoneId parent,
      List<BoneId> children,
      Transform bind,
      List<CubeIR> cubes) {
    this(id, name, parent, children, bind, cubes, "legacy/model", legacyLocation());
  }

  /**
   * Compatibility constructor for fixture/test data; production boundaries must provide a location.
   */
  @Deprecated
  public BoneIR(
      BoneId id,
      String name,
      BoneId parent,
      List<BoneId> children,
      Transform bind,
      List<CubeIR> cubes,
      String provenance) {
    this(
        id,
        name,
        parent,
        children,
        bind,
        cubes,
        provenance == null ? "legacy/model" : provenance,
        locationFor(provenance));
  }

  private static SourceLocation locationFor(String value) {
    return SourceLocation.of(
        new SourcePath(value == null || value.isBlank() ? "legacy/model" : value));
  }

  private static SourceLocation legacyLocation() {
    return locationFor("legacy/model");
  }
}
