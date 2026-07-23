package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public final class CpmStoreIdAssigner {
  public Result<CpmIdentifiedProjectionV1> assign(CpmStaticProjection projection) {
    return assign(projection, CpmStoreIdPolicy.defaults());
  }

  public Result<CpmIdentifiedProjectionV1> assign(
      CpmStaticProjection projection, CpmStoreIdPolicy policy) {
    if (projection == null || policy == null)
      return Result.failure(
          error(
              DiagnosticCodes.CPM_INVALID_STORE_ID,
              "projection and policy are required",
              Map.of()));
    var validation = new CpmLogicalProjectionValidator().validate(projection);
    if (validation.hasErrors()) return Result.failure(validation);
    var assignments = new ArrayList<CpmStoreIdAssignment>();
    for (var root : CpmVanillaRoot.values())
      assignments.add(
          new CpmStoreIdAssignment(
              new CpmNodeKey("root:" + root.id()),
              new CpmStoreId(root.reservedId()),
              CpmStoreIdKind.RESERVED_ROOT));
    var elements = new ArrayList<CpmLogicalElementV1>();
    for (var root : projection.project().roots())
      for (var child : root.children()) collect(child, elements);
    long available = policy.maxSafeId() - policy.firstGeneratedId() + 1;
    if (elements.size() > available)
      return Result.failure(
          error(
              DiagnosticCodes.CPM_INVALID_STORE_ID,
              "store ID range exhausted",
              Map.of(
                  "firstGeneratedId",
                  Long.toString(policy.firstGeneratedId()),
                  "maxSafeId",
                  Long.toString(policy.maxSafeId()),
                  "elementCount",
                  Integer.toString(elements.size()),
                  "available",
                  Long.toString(available),
                  "reason",
                  "store ID range exhausted")));
    long next = policy.firstGeneratedId();
    for (var e : elements)
      assignments.add(
          new CpmStoreIdAssignment(
              e.key(), new CpmStoreId(next++), CpmStoreIdKind.GENERATED_ELEMENT));
    var registry = CpmStoreIdRegistry.create(assignments);
    if (!registry.success()) return Result.failure(registry.diagnostics());
    var index = projection.index();
    var bones = new LinkedHashMap<BoneId, CpmStoreId>();
    for (var e : index.boneTargets().entrySet()) {
      var resolved = registry.value().resolve(e.getValue());
      if (!resolved.success()) return Result.failure(resolved.diagnostics());
      bones.put(e.getKey(), resolved.value());
    }
    var cubes = new LinkedHashMap<CubeId, CpmStoreId>();
    for (var e : index.cubeTargets().entrySet()) {
      var resolved = registry.value().resolve(e.getValue());
      if (!resolved.success()) return Result.failure(resolved.diagnostics());
      cubes.put(e.getKey(), resolved.value());
    }
    var helpers = new LinkedHashMap<CubeId, CpmStoreId>();
    for (var e : index.helperTargets().entrySet()) {
      var resolved = registry.value().resolve(CpmTargetRef.element(e.getValue()));
      if (!resolved.success()) return Result.failure(resolved.diagnostics());
      helpers.put(e.getKey(), resolved.value());
    }
    var identified =
        new CpmIdentifiedProjectionV1(
            projection, registry.value(), new CpmResolvedProjectionIndex(bones, cubes, helpers));
    var finalDiagnostics = new CpmStoreIdAssignmentValidator().validate(identified);
    return finalDiagnostics.hasErrors()
        ? Result.failure(finalDiagnostics)
        : Result.success(identified, finalDiagnostics);
  }

  private static void collect(CpmLogicalElementV1 e, List<CpmLogicalElementV1> out) {
    out.add(e);
    for (var child : e.children()) collect(child, out);
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
