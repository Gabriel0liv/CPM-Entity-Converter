package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SemanticRigMapTest {
  @Test
  void compiledMapIsImmutableAndContainsNoSourceLookups() {
    var bones = new LinkedHashMap<String, BoneId>();
    bones.put("head", new BoneId("head-id"));
    var map =
        new SemanticRigMap(
            bones,
            Map.of(),
            new CompiledRootRoles(Map.of()),
            null,
            Map.of(),
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null);
    bones.put("neck", new BoneId("neck-id"));
    assertFalse(map.bones().containsKey("neck"));
    assertThrows(UnsupportedOperationException.class, () -> map.bones().put("x", new BoneId("x")));
  }
}
