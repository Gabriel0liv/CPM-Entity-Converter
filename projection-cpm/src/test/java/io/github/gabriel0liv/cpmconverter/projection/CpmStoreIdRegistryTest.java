package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpmStoreIdRegistryTest {
  @Test
  void keepsReservedAndGeneratedOrderAndResolvesTargets() {
    var root =
        new CpmStoreIdAssignment(
            new CpmNodeKey("root:body"), new CpmStoreId(1), CpmStoreIdKind.RESERVED_ROOT);
    var element =
        new CpmStoreIdAssignment(
            new CpmNodeKey("bone:body"), new CpmStoreId(1000), CpmStoreIdKind.GENERATED_ELEMENT);
    var result = CpmStoreIdRegistry.create(List.of(root, element));
    assertTrue(result.success());
    var registry = result.value();
    assertEquals(
        new CpmStoreId(1), registry.resolve(CpmTargetRef.root(CpmVanillaRoot.BODY)).value());
    assertEquals(
        new CpmStoreId(1000),
        registry.resolve(CpmTargetRef.element(new CpmNodeKey("bone:body"))).value());
    assertEquals(List.of(root), registry.rootAssignments());
    assertEquals(List.of(element), registry.elementAssignments());
    assertThrows(UnsupportedOperationException.class, () -> registry.assignments().add(root));
    assertThrows(
        UnsupportedOperationException.class,
        () -> registry.byNodeKey().put(new CpmNodeKey("x"), new CpmStoreId(2)));
  }

  @Test
  void rejectsDuplicateIdsAndDanglingReferences() {
    var first =
        new CpmStoreIdAssignment(
            new CpmNodeKey("bone:a"), new CpmStoreId(1000), CpmStoreIdKind.GENERATED_ELEMENT);
    var second =
        new CpmStoreIdAssignment(
            new CpmNodeKey("bone:b"), new CpmStoreId(1000), CpmStoreIdKind.GENERATED_ELEMENT);
    var result = CpmStoreIdRegistry.create(List.of(first, second));
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream()
            .anyMatch(d -> d.code().value().equals("CPM_DUPLICATE_STORE_ID")));
    var valid = CpmStoreIdRegistry.create(List.of(first)).value();
    var dangling = valid.resolve(CpmTargetRef.element(new CpmNodeKey("bone:missing")));
    assertFalse(dangling.success());
    assertEquals("CPM_DANGLING_STORE_REF", dangling.diagnostics().all().get(0).code().value());
  }

  @Test
  void policyEnforcesSafeBounds() {
    assertEquals(1000, CpmStoreIdPolicy.defaults().firstGeneratedId());
    assertThrows(
        IllegalArgumentException.class, () -> new CpmStoreIdPolicy(999, CpmStoreId.MAX_SAFE_VALUE));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CpmStoreIdPolicy(1000, CpmStoreId.MAX_SAFE_VALUE + 1));
    assertThrows(
        IllegalArgumentException.class, () -> new CpmStoreId(CpmStoreId.MAX_SAFE_VALUE + 1));
  }
}
