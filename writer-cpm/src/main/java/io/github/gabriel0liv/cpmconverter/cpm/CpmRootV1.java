package io.github.gabriel0liv.cpmconverter.cpm;

import java.util.List;
import java.util.Objects;

/** One of the six CPM vanilla roots and its custom descendants. */
public record CpmRootV1(
    CpmVanillaPart vanillaPart,
    boolean showVanillaGeometry,
    boolean disableVanillaAnim,
    List<CpmElementV1> children) {
  public CpmRootV1 {
    Objects.requireNonNull(vanillaPart, "vanillaPart");
    children = List.copyOf(children == null ? List.of() : children);
  }
}
