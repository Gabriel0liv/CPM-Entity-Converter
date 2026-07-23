package io.github.gabriel0liv.cpmconverter.projection;

public record CpmStoreId(long value) implements Comparable<CpmStoreId> {
  public static final long MAX_SAFE_VALUE = 9_007_199_254_740_991L;

  public CpmStoreId {
    if (value < 0 || value > MAX_SAFE_VALUE) throw new IllegalArgumentException("store id");
  }

  @Override
  public int compareTo(CpmStoreId other) {
    return Long.compare(value, other.value);
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
