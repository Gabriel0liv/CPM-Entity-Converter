package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.FeatureOccurrence;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import java.util.List;

public record ParsedGeometry(
    SourcePath source,
    GeometryId geometryId,
    int textureWidth,
    int textureHeight,
    List<ParsedBone> bones,
    List<BoneId> roots,
    List<FeatureOccurrence> unsupportedFeatures) {
  public ParsedGeometry {
    bones = List.copyOf(bones);
    roots = List.copyOf(roots);
    unsupportedFeatures = List.copyOf(unsupportedFeatures);
  }
}
