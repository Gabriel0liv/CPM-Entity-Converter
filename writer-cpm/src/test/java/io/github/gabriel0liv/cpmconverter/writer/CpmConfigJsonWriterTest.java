package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import io.github.gabriel0liv.cpmconverter.projection.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmConfigJsonWriterTest {
  @Test
  void writesCanonicalTopLevelAndRootsWithoutPersistedRootIds() throws Exception {
    var identified = emptyProjection();
    var bytes = new CpmConfigJsonWriter().write(identified);
    assertTrue(bytes[bytes.length - 1] == '\n');
    var text = new String(bytes, StandardCharsets.UTF_8);
    assertFalse(text.contains("\r")); assertFalse(text.startsWith("\uFEFF"));
    var json = new ObjectMapper().readTree(bytes);
    assertEquals(1, json.get("version").intValue());
    assertEquals("default", json.get("skinType").textValue());
    assertEquals(32, json.at("/skinSize/x").intValue());
    assertEquals(32, json.at("/skinSize/y").intValue());
    assertEquals(6, json.get("elements").size());
    assertEquals(List.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg"),
        java.util.stream.StreamSupport.stream(json.get("elements").spliterator(), false).map(n -> n.get("id").textValue()).toList());
    for (var root : json.get("elements")) { assertFalse(root.has("storeID")); assertTrue(root.get("children").isArray()); }
    assertEquals(0, json.at("/textures/skin/anim").size());
  }

  private static CpmIdentifiedProjectionV1 emptyProjection() {
    var transform = new CpmTransformV1(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), new Vec3d(1, 1, 1));
    var roots = new ArrayList<CpmLogicalRootV1>(); var assignments = new ArrayList<CpmStoreIdAssignment>();
    for (var root : CpmVanillaRoot.values()) {
      roots.add(new CpmLogicalRootV1(root, transform, false, true, false, false, List.of(), CpmNodeOrigin.syntheticRoot(root)));
      assignments.add(new CpmStoreIdAssignment(new CpmNodeKey("root:" + root.id()), new CpmStoreId(root.reservedId()), CpmStoreIdKind.RESERVED_ROOT));
    }
    var project = new CpmLogicalProjectV1(1, "default", new CpmLogicalTextureV1("skin.png", 32, 32, "default", false), roots);
    var logical = new CpmStaticProjection(project, new CpmProjectionIndex(Map.of(), Map.of(), Map.of()));
    var registry = CpmStoreIdRegistry.create(assignments).value();
    return new CpmIdentifiedProjectionV1(logical, registry, new CpmResolvedProjectionIndex(Map.of(), Map.of(), Map.of()));
  }
}
