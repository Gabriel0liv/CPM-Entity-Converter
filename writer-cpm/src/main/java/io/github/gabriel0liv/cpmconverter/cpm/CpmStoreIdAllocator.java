package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Allocates persistent CPM V1 store IDs from canonical tree order. */
public final class CpmStoreIdAllocator {
  public static final long FIRST_GENERATED_ID = 1000L;
  public static final long MAX_SAFE_ID = 9_007_199_254_740_991L;
  private static final long RESERVED_MAX_ID = 6L;

  private final long firstGeneratedId;

  public CpmStoreIdAllocator() {
    this(FIRST_GENERATED_ID);
  }

  CpmStoreIdAllocator(long firstGeneratedId) {
    if (firstGeneratedId <= RESERVED_MAX_ID || firstGeneratedId > MAX_SAFE_ID) {
      throw new IllegalArgumentException("firstGeneratedId");
    }
    this.firstGeneratedId = firstGeneratedId;
  }

  public Result<CpmStoreIdPlan> allocate(CpmStaticProjectV1 project) {
    if (project == null) {
      return failure(DiagnosticCodes.INTERNAL_ERROR, "project is null");
    }

    EnumMap<CpmVanillaPart, Long> rootIds = new EnumMap<>(CpmVanillaPart.class);
    EnumMap<CpmVanillaPart, CpmRootV1> roots = new EnumMap<>(CpmVanillaPart.class);
    for (CpmRootV1 root : project.roots()) {
      if (roots.putIfAbsent(root.vanillaPart(), root) != null) {
        return failure(
            DiagnosticCodes.INTERNAL_ERROR, "duplicate CPM root " + root.vanillaPart().name());
      }
    }
    for (CpmVanillaPart part : CpmVanillaPart.values()) {
      rootIds.put(part, (long) part.ordinal());
      if (!roots.containsKey(part)) {
        return failure(DiagnosticCodes.INTERNAL_ERROR, "missing CPM root " + part.name());
      }
    }

    LinkedHashMap<ProjectionKey, Long> elementIds = new LinkedHashMap<>();
    Cursor cursor = new Cursor(firstGeneratedId);
    try {
      for (CpmVanillaPart part : CpmVanillaPart.values()) {
        for (CpmElementV1 child : roots.get(part).children()) {
          allocatePreorder(child, cursor, elementIds);
        }
      }
    } catch (StoreIdRangeException exception) {
      return failure(
          DiagnosticCodes.CPM_STORE_ID_RANGE,
          "generated CPM storeID would exceed JavaScript safe integer range");
    } catch (IllegalStateException exception) {
      return failure(DiagnosticCodes.INTERNAL_ERROR, exception.getMessage());
    }

    return Result.success(new CpmStoreIdPlan(rootIds, elementIds));
  }

  private void allocatePreorder(
      CpmElementV1 element, Cursor cursor, Map<ProjectionKey, Long> elementIds) {
    long storeId = cursor.take();
    if (elementIds.putIfAbsent(element.key(), storeId) != null) {
      throw new IllegalStateException("duplicate projection key " + element.key().value());
    }
    for (CpmElementV1 child : element.children()) {
      allocatePreorder(child, cursor, elementIds);
    }
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }

  private static final class Cursor {
    private long next;

    private Cursor(long next) {
      this.next = next;
    }

    private long take() {
      if (next > MAX_SAFE_ID) throw new StoreIdRangeException();
      return next++;
    }
  }

  private static final class StoreIdRangeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
