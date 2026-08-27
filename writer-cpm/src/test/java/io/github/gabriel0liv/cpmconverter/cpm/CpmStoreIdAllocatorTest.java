package io.github.gabriel0liv.cpmconverter.cpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CpmStoreIdAllocatorTest {
  @Test
  void assignsVanillaAndGeneratedIdsInCanonicalPreorder() {
    ProjectionKey entityKey = ProjectionKey.entityRoot();
    ProjectionKey firstKey = new ProjectionKey("BONE:first");
    ProjectionKey nestedKey = new ProjectionKey("CUBE:nested");
    ProjectionKey secondKey = new ProjectionKey("BONE:second");

    CpmElementV1 nested = structural(nestedKey, "nested", List.of());
    CpmElementV1 first = structural(firstKey, "first", List.of(nested));
    CpmElementV1 second = structural(secondKey, "second", List.of());
    CpmElementV1 entityRoot = structural(entityKey, "entity_root", List.of(first, second));
    CpmStaticProjectV1 project =
        project(entityRoot, reverseTargets(entityRoot, first, nested, second));

    var result = new CpmStoreIdAllocator().allocate(project);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    CpmStoreIdPlan ids = result.value();
    assertEquals(0L, ids.rootId(CpmVanillaPart.HEAD));
    assertEquals(1L, ids.rootId(CpmVanillaPart.BODY));
    assertEquals(5L, ids.rootId(CpmVanillaPart.RIGHT_LEG));
    assertEquals(1000L, ids.elementId(entityKey));
    assertEquals(1001L, ids.elementId(firstKey));
    assertEquals(1002L, ids.elementId(nestedKey));
    assertEquals(1003L, ids.elementId(secondKey));
    assertEquals(List.of(1000L, 1001L, 1002L, 1003L), ids.elementIds().values().stream().toList());
    assertTrue(
        ids.elementIds().values().stream()
            .allMatch(id -> id > 6 && id <= CpmStoreIdAllocator.MAX_SAFE_ID));
  }

  @Test
  void equivalentTreesGetSameIdsEvenWhenLogicalTargetMapOrderDiffers() {
    ProjectionKey entityKey = ProjectionKey.entityRoot();
    ProjectionKey childKey = new ProjectionKey("BONE:child");
    CpmElementV1 child = structural(childKey, "child", List.of());
    CpmElementV1 entityRoot = structural(entityKey, "entity_root", List.of(child));

    LinkedHashMap<ProjectionKey, CpmElementV1> forward = new LinkedHashMap<>();
    forward.put(entityKey, entityRoot);
    forward.put(childKey, child);
    LinkedHashMap<ProjectionKey, CpmElementV1> reverse = new LinkedHashMap<>();
    reverse.put(childKey, child);
    reverse.put(entityKey, entityRoot);

    var first = new CpmStoreIdAllocator().allocate(project(entityRoot, forward));
    var second = new CpmStoreIdAllocator().allocate(project(entityRoot, reverse));

    assertTrue(first.success());
    assertTrue(second.success());
    assertEquals(first.value(), second.value());
    assertThrows(
        IllegalArgumentException.class,
        () -> first.value().elementId(new ProjectionKey("BONE:missing")));
  }

  @Test
  void rejectsIdsAboveJavascriptSafeIntegerRange() {
    ProjectionKey entityKey = ProjectionKey.entityRoot();
    ProjectionKey childKey = new ProjectionKey("BONE:child");
    CpmElementV1 child = structural(childKey, "child", List.of());
    CpmElementV1 entityRoot = structural(entityKey, "entity_root", List.of(child));
    CpmStaticProjectV1 project =
        project(entityRoot, Map.of(entityKey, entityRoot, childKey, child));

    var result = new CpmStoreIdAllocator(CpmStoreIdAllocator.MAX_SAFE_ID).allocate(project);

    assertFalse(result.success());
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.CPM_STORE_ID_RANGE)));
  }

  private static CpmStaticProjectV1 project(
      CpmElementV1 entityRoot, Map<ProjectionKey, CpmElementV1> logicalTargets) {
    return new CpmStaticProjectV1(
        List.of(
            root(CpmVanillaPart.HEAD, List.of()),
            root(CpmVanillaPart.BODY, List.of(entityRoot)),
            root(CpmVanillaPart.LEFT_ARM, List.of()),
            root(CpmVanillaPart.RIGHT_ARM, List.of()),
            root(CpmVanillaPart.LEFT_LEG, List.of()),
            root(CpmVanillaPart.RIGHT_LEG, List.of())),
        List.of(),
        logicalTargets);
  }

  private static CpmRootV1 root(CpmVanillaPart part, List<CpmElementV1> children) {
    return new CpmRootV1(part, false, false, children);
  }

  private static LinkedHashMap<ProjectionKey, CpmElementV1> reverseTargets(
      CpmElementV1 entityRoot, CpmElementV1 first, CpmElementV1 nested, CpmElementV1 second) {
    LinkedHashMap<ProjectionKey, CpmElementV1> targets = new LinkedHashMap<>();
    targets.put(second.key(), second);
    targets.put(nested.key(), nested);
    targets.put(first.key(), first);
    targets.put(entityRoot.key(), entityRoot);
    return targets;
  }

  private static CpmElementV1 structural(
      ProjectionKey key, String name, List<CpmElementV1> children) {
    return new CpmElementV1(
        key,
        name,
        CpmElementKind.BONE,
        CpmTransformV1.identity(),
        Vec3d.ZERO,
        Vec3d.ZERO,
        0,
        false,
        false,
        true,
        false,
        null,
        children);
  }
}
