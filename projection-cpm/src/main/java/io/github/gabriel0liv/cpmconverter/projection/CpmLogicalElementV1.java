package io.github.gabriel0liv.cpmconverter.projection;

import java.util.*;

public record CpmLogicalElementV1(
    CpmNodeKey key,
    CpmNodeKind kind,
    String name,
    CpmTransformV1 transform,
    CpmLogicalCubeV1 cube,
    List<CpmLogicalElementV1> children,
    CpmNodeOrigin origin) {
  public CpmLogicalElementV1 {
    if (key == null || kind == null || name == null || transform == null || origin == null)
      throw new IllegalArgumentException("element");
    children = List.copyOf(children == null ? List.of() : children);
  }
}
