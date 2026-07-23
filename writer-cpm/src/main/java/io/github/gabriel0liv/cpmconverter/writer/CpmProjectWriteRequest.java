package io.github.gabriel0liv.cpmconverter.writer;

import io.github.gabriel0liv.cpmconverter.projection.CpmIdentifiedProjectionV1;

public record CpmProjectWriteRequest(CpmIdentifiedProjectionV1 projection, byte[] skinPng) {
  public CpmProjectWriteRequest {
    if (skinPng != null) skinPng = skinPng.clone();
  }

  @Override
  public byte[] skinPng() {
    return skinPng == null ? null : skinPng.clone();
  }
}
