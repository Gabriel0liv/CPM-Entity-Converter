package io.github.gabriel0liv.cpmconverter.ir;

import java.util.*;

public record ModelIR(
    SourceDescriptor source,
    GeometryId geometryId,
    List<BoneIR> bones,
    List<BoneId> roots,
    List<AnimationClipIR> clips,
    List<TextureIR> textures,
    List<FeatureOccurrence> unsupportedFeatures) {
  public ModelIR {
    if (source == null) throw new IllegalArgumentException("source");
    if (geometryId == null) throw new IllegalArgumentException("geometry id");
    bones = List.copyOf(bones == null ? List.of() : bones);
    roots = List.copyOf(roots == null ? List.of() : roots);
    clips = List.copyOf(clips == null ? List.of() : clips);
    textures = List.copyOf(textures == null ? List.of() : textures);
    unsupportedFeatures =
        List.copyOf(unsupportedFeatures == null ? List.of() : unsupportedFeatures);
  }

  public ModelIR(
      SourceDescriptor source,
      List<BoneIR> bones,
      List<BoneId> roots,
      List<AnimationClipIR> clips,
      List<TextureIR> textures) {
    this(source, new GeometryId("default"), bones, roots, clips, textures, List.of());
  }
}
