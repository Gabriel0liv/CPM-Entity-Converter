package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

public final class CpmStoreIdRegistry {
  private final List<CpmStoreIdAssignment> assignments;
  private final Map<CpmNodeKey, CpmStoreId> byNodeKey;
  private final Map<CpmStoreId, CpmNodeKey> byStoreId;

  private CpmStoreIdRegistry(List<CpmStoreIdAssignment> values) {
    assignments = List.copyOf(values);
    var nodes = new LinkedHashMap<CpmNodeKey, CpmStoreId>();
    var ids = new LinkedHashMap<CpmStoreId, CpmNodeKey>();
    for (var a : assignments) {
      nodes.put(a.nodeKey(), a.storeId());
      ids.put(a.storeId(), a.nodeKey());
    }
    byNodeKey = Collections.unmodifiableMap(nodes);
    byStoreId = Collections.unmodifiableMap(ids);
  }

  public static Result<CpmStoreIdRegistry> create(List<CpmStoreIdAssignment> input) {
    if (input == null)
      return Result.failure(
          error(DiagnosticCodes.CPM_INVALID_STORE_ID, "assignments are required", Map.of()));
    var bag = new DiagnosticBag();
    var nodes = new HashSet<CpmNodeKey>();
    var ids = new HashSet<CpmStoreId>();
    for (var a : input) {
      if (a == null || !nodes.add(a.nodeKey()))
        bag = bag.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID, "duplicate node key", Map.of()));
      else if (!ids.add(a.storeId()))
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_DUPLICATE_STORE_ID,
                    "duplicate store id",
                    Map.of("storeId", Long.toString(a.storeId().value()))));
      if (a != null
          && a.kind() == CpmStoreIdKind.RESERVED_ROOT
          && (a.storeId().value() > 5 || !a.nodeKey().value().startsWith("root:")))
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "invalid reserved root assignment",
                    Map.of("nodeKey", a.nodeKey().value())));
      if (a != null
          && a.kind() == CpmStoreIdKind.GENERATED_ELEMENT
          && (a.storeId().value() < 1000 || a.nodeKey().value().startsWith("root:")))
        bag =
            bag.add(
                error(
                    DiagnosticCodes.CPM_INVALID_STORE_ID,
                    "invalid generated assignment",
                    Map.of("nodeKey", a.nodeKey().value())));
    }
    return bag.hasErrors() ? Result.failure(bag) : Result.success(new CpmStoreIdRegistry(input));
  }

  public List<CpmStoreIdAssignment> assignments() {
    return assignments;
  }

  public Map<CpmNodeKey, CpmStoreId> byNodeKey() {
    return byNodeKey;
  }

  public Map<CpmStoreId, CpmNodeKey> byStoreId() {
    return byStoreId;
  }

  public Optional<CpmStoreId> findByNode(CpmNodeKey key) {
    return Optional.ofNullable(byNodeKey.get(key));
  }

  public Optional<CpmNodeKey> findByStoreId(CpmStoreId id) {
    return Optional.ofNullable(byStoreId.get(id));
  }

  public List<CpmStoreIdAssignment> rootAssignments() {
    return assignments.stream().filter(a -> a.kind() == CpmStoreIdKind.RESERVED_ROOT).toList();
  }

  public List<CpmStoreIdAssignment> elementAssignments() {
    return assignments.stream().filter(a -> a.kind() == CpmStoreIdKind.GENERATED_ELEMENT).toList();
  }

  public Result<CpmStoreId> resolve(CpmTargetRef target) {
    if (target == null)
      return Result.failure(
          error(DiagnosticCodes.CPM_DANGLING_STORE_REF, "target is missing", Map.of()));
    var a =
        assignments.stream().filter(x -> x.nodeKey().equals(target.key())).findFirst().orElse(null);
    if (a == null || (target.root() != (a.kind() == CpmStoreIdKind.RESERVED_ROOT)))
      return Result.failure(
          error(
              DiagnosticCodes.CPM_DANGLING_STORE_REF,
              "target has no compatible store id",
              Map.of(
                  "targetKey",
                  target.key().value(),
                  "targetRoot",
                  Boolean.toString(target.root()))));
    return Result.success(a.storeId());
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
