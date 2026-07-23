package io.github.gabriel0liv.cpmconverter.projection;

public record CpmTargetRef(CpmNodeKey key, boolean root) {
  public static CpmTargetRef root(CpmVanillaRoot r) {
    return new CpmTargetRef(new CpmNodeKey("root:" + r.id()), true);
  }

  public static CpmTargetRef element(CpmNodeKey k) {
    return new CpmTargetRef(k, false);
  }
}
