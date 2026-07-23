package io.github.gabriel0liv.cpmconverter.writer;

import java.util.Arrays;

public final class CpmProjectArtifact {
  private final byte[] bytes;

  private CpmProjectArtifact(byte[] bytes) {
    this.bytes = bytes.clone();
  }

  public static CpmProjectArtifact of(byte[] bytes) {
    if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("bytes");
    return new CpmProjectArtifact(bytes);
  }

  public byte[] bytes() { return bytes.clone(); }
  public int size() { return bytes.length; }

  @Override public boolean equals(Object o) { return o instanceof CpmProjectArtifact a && Arrays.equals(bytes, a.bytes); }
  @Override public int hashCode() { return Arrays.hashCode(bytes); }
  @Override public String toString() { return "CpmProjectArtifact[size=" + bytes.length + "]"; }
}
