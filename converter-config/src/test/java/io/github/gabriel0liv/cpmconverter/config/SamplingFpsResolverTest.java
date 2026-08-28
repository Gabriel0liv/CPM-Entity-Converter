package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import org.junit.jupiter.api.Test;

class SamplingFpsResolverTest {
  @Test
  void stateOverrideWinsOverGlobal() {
    var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, 40);

    assertEquals(40, SamplingFpsResolver.resolve(state, new CompiledSamplingPolicy(20)));
  }

  @Test
  void globalWinsWhenStateHasNoOverride() {
    var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, null);

    assertEquals(30, SamplingFpsResolver.resolve(state, new CompiledSamplingPolicy(30)));
  }

  @Test
  void defaultIsExactlyTwenty() {
    var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, null);

    assertEquals(20, SamplingFpsResolver.resolve(state, null));
  }

  @Test
  void globalAppliesWithoutStateMapping() {
    assertEquals(60, SamplingFpsResolver.resolve(null, new CompiledSamplingPolicy(60)));
  }

  @Test
  void defaultAppliesWithoutStateOrGlobalPolicy() {
    assertEquals(20, SamplingFpsResolver.resolve(null, null));
  }
}
