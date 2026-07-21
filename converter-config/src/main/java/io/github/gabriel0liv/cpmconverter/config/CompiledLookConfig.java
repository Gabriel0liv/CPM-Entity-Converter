package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import java.util.Optional;

public record CompiledLookConfig(
    Optional<BoneId> head,
    Optional<BoneId> neck,
    String composition,
    double neckInfluence,
    double headInfluence,
    boolean allowOverrotation) {
  public CompiledLookConfig {
    head = head == null ? Optional.empty() : head;
    neck = neck == null ? Optional.empty() : neck;
    composition = composition == null ? "independent" : composition;
  }
}
