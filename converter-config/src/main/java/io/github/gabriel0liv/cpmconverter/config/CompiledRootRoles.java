package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CompiledRootRoles(Map<String, BoneId> roles) {
  public CompiledRootRoles {
    roles = Collections.unmodifiableMap(new LinkedHashMap<>(roles == null ? Map.of() : roles));
  }
}
