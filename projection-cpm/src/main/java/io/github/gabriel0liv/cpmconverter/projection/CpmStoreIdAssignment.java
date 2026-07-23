package io.github.gabriel0liv.cpmconverter.projection;

public record CpmStoreIdAssignment(CpmNodeKey nodeKey, CpmStoreId storeId, CpmStoreIdKind kind) {
  public CpmStoreIdAssignment {
    if (nodeKey == null || storeId == null || kind == null)
      throw new IllegalArgumentException("assignment");
  }
}
