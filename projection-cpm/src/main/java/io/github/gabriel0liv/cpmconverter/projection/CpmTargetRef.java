package io.github.gabriel0liv.cpmconverter.projection;

public record CpmTargetRef(CpmNodeKey key, boolean root) {
  public CpmTargetRef {
    if (key == null
        || (root && !key.value().startsWith("root:"))
        || (!root && key.value().startsWith("root:"))) throw new IllegalArgumentException("target");
  }

  public static CpmTargetRef root(CpmVanillaRoot r) {
    return new CpmTargetRef(new CpmNodeKey("root:" + r.id()), true);
  }

  public static CpmTargetRef element(CpmNodeKey k) {
    return new CpmTargetRef(k, false);
  }
}
