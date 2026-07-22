package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;

public record ParsedBone(
    BoneId id,
    String sourceName,
    BoneId parent,
    List<BoneId> children,
    Transform bindLocal,
    List<ParsedCube> cubes,
    SourceLocation source) {
  public ParsedBone {
    children = List.copyOf(children);
    cubes = List.copyOf(cubes);
  }
}
