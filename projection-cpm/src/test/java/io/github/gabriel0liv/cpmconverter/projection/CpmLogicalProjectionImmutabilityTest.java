package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmLogicalProjectionImmutabilityTest {
  @Test
  void defensiveCopiesAndTargetInvariants() {
    var roots = new ArrayList<CpmLogicalRootV1>();
    var texture = new CpmLogicalTextureV1("texture.png", 32, 32, "default", false);
    var p = new CpmLogicalProjectV1(1, "default", texture, roots);
    roots.add(
        new CpmLogicalRootV1(
            CpmVanillaRoot.HEAD,
            new CpmTransformV1(Vec3d.ZERO, Vec3d.ZERO, new Vec3d(1, 1, 1)),
            false,
            true,
            false,
            false,
            List.of(),
            CpmNodeOrigin.syntheticRoot(CpmVanillaRoot.HEAD)));
    assertTrue(p.roots().isEmpty());
    var map = new LinkedHashMap<BoneId, CpmTargetRef>();
    map.put(new BoneId("b"), CpmTargetRef.element(new CpmNodeKey("bone:b")));
    var index = new CpmProjectionIndex(map, Map.of(), Map.of());
    map.clear();
    assertEquals(1, index.boneTargets().size());
    assertThrows(UnsupportedOperationException.class, () -> index.boneTargets().clear());
    assertThrows(
        IllegalArgumentException.class, () -> new CpmTargetRef(new CpmNodeKey("root:x"), false));
  }
}
