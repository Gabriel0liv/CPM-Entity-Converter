package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable static CPM graph before deterministic IDs and archive serialization. */
public record CpmStaticProjectV1(
    List<CpmRootV1> roots,
    List<TextureIR> textures,
    Map<ProjectionKey, CpmElementV1> logicalTargets) {
  public CpmStaticProjectV1 {
    roots = List.copyOf(roots == null ? List.of() : roots);
    textures = List.copyOf(textures == null ? List.of() : textures);
    logicalTargets =
        Collections.unmodifiableMap(
            new LinkedHashMap<>(logicalTargets == null ? Map.of() : logicalTargets));
  }
}
