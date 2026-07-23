package io.github.gabriel0liv.cpmconverter.projection;

public record CpmStoreIdPolicy(long firstGeneratedId, long maxSafeId) {
  public CpmStoreIdPolicy {
    if (firstGeneratedId < 1000
        || maxSafeId > CpmStoreId.MAX_SAFE_VALUE
        || firstGeneratedId > maxSafeId)
      throw new IllegalArgumentException("invalid store id policy");
  }

  public static CpmStoreIdPolicy defaults() {
    return new CpmStoreIdPolicy(1000, CpmStoreId.MAX_SAFE_VALUE);
  }
}
