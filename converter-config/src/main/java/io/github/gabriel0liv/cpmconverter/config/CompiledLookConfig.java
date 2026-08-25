package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record CompiledLookConfig(
    Optional<BoneId> head,
    Optional<BoneId> neck,
    String composition,
    double neckInfluence,
    double headInfluence,
    boolean allowOverrotation,
    Map<String, Double> limits) {
  public CompiledLookConfig {
    head = head == null ? Optional.empty() : head;
    neck = neck == null ? Optional.empty() : neck;
    composition = composition == null ? "independent" : composition;
    limits = Collections.unmodifiableMap(new TreeMap<>(limits == null ? Map.of() : limits));
  }
}
