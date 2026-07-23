package io.github.gabriel0liv.cpmconverter.projection;

public record CpmStaticProjection(CpmLogicalProjectV1 project, CpmProjectionIndex index) {
  public CpmStaticProjection {
    if (project == null || index == null) throw new IllegalArgumentException("projection");
  }
}
