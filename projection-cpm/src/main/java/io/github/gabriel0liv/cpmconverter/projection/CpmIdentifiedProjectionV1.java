package io.github.gabriel0liv.cpmconverter.projection;

public record CpmIdentifiedProjectionV1(
    CpmStaticProjection logicalProjection,
    CpmStoreIdRegistry storeIds,
    CpmResolvedProjectionIndex resolvedIndex) {
  public CpmIdentifiedProjectionV1 {
    if (logicalProjection == null || storeIds == null || resolvedIndex == null)
      throw new IllegalArgumentException("identified projection");
  }
}
