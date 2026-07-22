package io.github.gabriel0liv.cpmconverter.geckolib;

/** Raw, canonical JSON boundary retained for T201; no UV interpretation occurs in T200. */
public record RawUvBoundary(String canonicalJson) {
  public RawUvBoundary {
    if (canonicalJson == null || canonicalJson.isBlank()) {
      throw new IllegalArgumentException("raw UV boundary");
    }
  }
}
