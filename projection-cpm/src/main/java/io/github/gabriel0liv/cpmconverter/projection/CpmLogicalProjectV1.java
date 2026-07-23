package io.github.gabriel0liv.cpmconverter.projection;

import java.util.*;

public record CpmLogicalProjectV1(
    int version, String skinType, CpmLogicalTextureV1 texture, List<CpmLogicalRootV1> roots) {
  public CpmLogicalProjectV1 {
    if (version != 1 || skinType == null || texture == null)
      throw new IllegalArgumentException("project");
    roots = List.copyOf(roots == null ? List.of() : roots);
  }
}
