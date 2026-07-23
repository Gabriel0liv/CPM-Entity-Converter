package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public final class CpmStoreIdAssignmentValidator {
  public DiagnosticBag validate(CpmIdentifiedProjectionV1 identified) {
    var bag = new DiagnosticBag();
    if (identified == null
        || identified.logicalProjection() == null
        || identified.storeIds() == null
        || identified.resolvedIndex() == null)
      return bag.add(
          error(
              DiagnosticCodes.CPM_INVALID_STORE_ID,
              "identified projection is incomplete",
              Map.of()));
    var registry = identified.storeIds();
    var roots = registry.rootAssignments();
    if (roots.size() != 6)
      bag =
          bag.add(
              error(
                  DiagnosticCodes.CPM_INVALID_STORE_ID,
                  "six reserved roots are required",
                  Map.of()));
    for (int i = 0; i < Math.min(6, roots.size()); i++) {
      var expected = CpmVanillaRoot.values()[i];
      var a = roots.get(i);
      if (!a.nodeKey().value().equals("root:" + expected.id())
          || a.storeId().value() != expected.reservedId())
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "root assignment is not canonical",
                    Map.of("index", Integer.toString(i))));
    }
    var generated = registry.elementAssignments();
    long expected = generated.isEmpty() ? 1000 : generated.get(0).storeId().value();
    for (var a : generated) {
      if (a.storeId().value() < 1000
          || a.storeId().value() > CpmStoreId.MAX_SAFE_VALUE
          || a.storeId().value() != expected)
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "generated IDs must be contiguous and safe",
                    Map.of("storeId", Long.toString(a.storeId().value()))));
      expected++;
    }
    var p = identified.logicalProjection();
    var keys = new HashSet<CpmNodeKey>();
    for (var r : p.project().roots()) for (var e : r.children()) collect(e, keys);
    for (var k : keys)
      if (registry.findByNode(k).isEmpty())
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "element is missing a store ID",
                    Map.of("nodeKey", k.value())));
    for (var a : generated)
      if (!keys.contains(a.nodeKey()))
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "assignment references an unknown element",
                    Map.of("nodeKey", a.nodeKey().value())));
    var bi = identified.resolvedIndex().boneStoreIds();
    var ci = identified.resolvedIndex().cubeStoreIds();
    var hi = identified.resolvedIndex().helperStoreIds();
    for (var e : bi.entrySet()) bag = checkIndex(e.getValue(), registry, bag, "bone");
    for (var e : ci.entrySet()) bag = checkIndex(e.getValue(), registry, bag, "cube");
    for (var e : hi.entrySet()) bag = checkIndex(e.getValue(), registry, bag, "helper");
    return bag;
  }

  private static void collect(CpmLogicalElementV1 e, Set<CpmNodeKey> out) {
    out.add(e.key());
    for (var c : e.children()) collect(c, out);
  }

  private static DiagnosticBag checkIndex(
      CpmStoreId id, CpmStoreIdRegistry registry, DiagnosticBag bag, String kind) {
    return id == null || registry.findByStoreId(id).isEmpty()
        ? bag.add(
            error(
                DiagnosticCodes.CPM_DANGLING_STORE_REF,
                "numeric index does not resolve",
                Map.of("kind", kind)))
        : bag;
  }

  private static Diagnostic error(String code, String message, Map<String, String> context) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        null,
        message,
        "repair the store ID assignment",
        null,
        null,
        new TreeMap<>(context));
  }
}
