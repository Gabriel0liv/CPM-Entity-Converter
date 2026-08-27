package io.github.gabriel0liv.cpmconverter.verification;

import io.github.gabriel0liv.cpmconverter.cpm.CpmStaticProjectV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStoreIdPlan;

public record CurrentFixtureArtifact(
    String fixture,
    CpmStaticProjectV1 project,
    CpmStoreIdPlan storeIds,
    byte[] bytes,
    String sha256) {
  public CurrentFixtureArtifact {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
