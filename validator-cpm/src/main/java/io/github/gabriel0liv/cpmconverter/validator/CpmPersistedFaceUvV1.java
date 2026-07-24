package io.github.gabriel0liv.cpmconverter.validator;
public record CpmPersistedFaceUvV1(int sx, int sy, int ex, int ey, CpmPersistedUvRotation rotation, boolean autoUv) {
  public CpmPersistedFaceUvV1 { if (rotation == null) throw new NullPointerException("rotation"); }
}
