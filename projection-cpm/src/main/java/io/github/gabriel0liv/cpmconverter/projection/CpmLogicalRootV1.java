package io.github.gabriel0liv.cpmconverter.projection;

import java.util.*;

public record CpmLogicalRootV1(
    CpmVanillaRoot root,
    CpmTransformV1 transform,
    boolean show,
    boolean showInEditor,
    boolean locked,
    boolean disableVanillaAnim,
    List<CpmLogicalElementV1> children,
    CpmNodeOrigin origin) {
  public CpmLogicalRootV1 {
    if (root == null || transform == null || origin == null)
      throw new IllegalArgumentException("root");
    children = List.copyOf(children == null ? List.of() : children);
  }
}
