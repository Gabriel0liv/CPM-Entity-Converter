package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;

public record BoneIR(
    BoneId id,
    String name,
    BoneId parent,
    List<BoneId> children,
    Transform bind,
    List<CubeIR> cubes,
    SourceLocation provenance) {
  public BoneIR {
    if (id == null || name == null || bind == null) throw new IllegalArgumentException("bone");
    children = List.copyOf(children == null ? List.of() : children);
    cubes = List.copyOf(cubes == null ? List.of() : cubes);
    if (provenance == null) throw new IllegalArgumentException("provenance");
  }

  /** Test-only convenience constructor; production boundaries must provide a location. */
  BoneIR(
      BoneId id,
      String name,
      BoneId parent,
      List<BoneId> children,
      Transform bind,
      List<CubeIR> cubes) {
    this(id, name, parent, children, bind, cubes, SourceLocation.of(new SourcePath("test/model")));
  }

  /* Explicit migration constructor retained package-private for fixture tests only. */
  BoneIR(
      BoneId id,
      String name,
      BoneId parent,
      List<BoneId> children,
      Transform bind,
      List<CubeIR> cubes,
      String provenance) {
    this(id, name, parent, children, bind, cubes, SourceLocation.of(new SourcePath(provenance)));
  }
}
