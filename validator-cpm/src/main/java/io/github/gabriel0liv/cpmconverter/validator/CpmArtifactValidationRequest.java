package io.github.gabriel0liv.cpmconverter.validator;

import java.util.Arrays;

public record CpmArtifactValidationRequest(byte[] artifactBytes, CpmArtifactLimits limits) {
  public CpmArtifactValidationRequest { artifactBytes = artifactBytes == null ? null : artifactBytes.clone(); if (artifactBytes == null) throw new IllegalArgumentException("artifactBytes"); if (limits == null) throw new IllegalArgumentException("limits"); }
  @Override public byte[] artifactBytes() { return artifactBytes.clone(); }
  public static CpmArtifactValidationRequest of(byte[] bytes) { return new CpmArtifactValidationRequest(bytes, CpmArtifactLimits.defaults()); }
}
