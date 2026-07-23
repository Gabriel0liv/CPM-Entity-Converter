package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.*;

/** Defensive validator for the in-memory T300 graph and its source index. */
public final class CpmLogicalProjectionValidator {
  public DiagnosticBag validate(CpmStaticProjection projection, ModelIR source, BoneId anchor) {
    DiagnosticBag bag = validate(projection);
    if (source == null || anchor == null)
      return bag.add(failure("source model and anchor are required"));
    if (projection == null) return bag;
    var expectedBones = source.bones().stream().map(BoneIR::id).toList();
    var expectedCubes =
        source.bones().stream().flatMap(b -> b.cubes().stream()).map(CubeIR::id).toList();
    if (!new ArrayList<>(projection.index().boneTargets().keySet()).equals(expectedBones))
      bag = bag.add(failure("bone target order does not match ModelIR"));
    if (!new ArrayList<>(projection.index().cubeTargets().keySet()).equals(expectedCubes))
      bag = bag.add(failure("cube target order does not match ModelIR"));
    if (!projection.index().boneTargets().keySet().equals(new LinkedHashSet<>(expectedBones)))
      bag = bag.add(failure("bone target set does not match ModelIR"));
    if (!projection.index().cubeTargets().keySet().equals(new LinkedHashSet<>(expectedCubes)))
      bag = bag.add(failure("cube target set does not match ModelIR"));
    var body =
        projection.project().roots().stream()
            .filter(r -> r.root() == CpmVanillaRoot.BODY)
            .findFirst()
            .orElse(null);
    if (body == null
        || body.origin().kind() != CpmNodeOriginKind.SOURCE_BONE
        || !anchor.equals(body.origin().boneId()))
      bag = bag.add(failure("BODY origin does not identify the anchor"));
    for (var e : projection.index().boneTargets().entrySet()) {
      var ref = e.getValue();
      if (e.getKey().equals(anchor)) {
        if (!ref.root() || !ref.key().value().equals("root:body"))
          bag = bag.add(failure("anchor target is not root:body"));
      } else {
        var n = find(projection.project(), ref.key());
        if (n == null
            || n.kind() != CpmNodeKind.BONE
            || n.origin().boneId() == null
            || !e.getKey().equals(n.origin().boneId()))
          bag = bag.add(failure("bone target does not resolve to its bone node"));
      }
    }
    for (var e : projection.index().cubeTargets().entrySet()) {
      var n = find(projection.project(), e.getValue().key());
      if (n == null
          || n.kind() != CpmNodeKind.CUBE
          || n.origin().cubeId() == null
          || !e.getKey().equals(n.origin().cubeId()))
        bag = bag.add(failure("cube target does not resolve to a cube node"));
    }
    for (var e : projection.index().helperTargets().entrySet()) {
      var n = find(projection.project(), e.getValue());
      if (n == null
          || n.kind() != CpmNodeKind.CUBE_ROTATION_HELPER
          || n.origin().cubeId() == null
          || !e.getKey().equals(n.origin().cubeId()))
        bag = bag.add(failure("helper target does not resolve to its helper"));
    }
    return bag;
  }

  public DiagnosticBag validate(
      CpmStaticProjection projection, Set<BoneId> expectedBones, Set<CubeId> expectedCubes) {
    DiagnosticBag bag = validate(projection);
    if (projection != null) {
      if (!projection.index().boneTargets().keySet().equals(expectedBones))
        bag = bag.add(failure("bone target set does not match source"));
      if (!projection.index().cubeTargets().keySet().equals(expectedCubes))
        bag = bag.add(failure("cube target set does not match source"));
    }
    return bag;
  }

  public DiagnosticBag validate(CpmStaticProjection projection) {
    DiagnosticBag bag = new DiagnosticBag();
    if (projection == null || projection.project() == null)
      return bag.add(failure("projection is null"));
    var p = projection.project();
    if (p.version() != 1 || p.roots().size() != 6)
      return bag.add(failure("CPM V1 requires six roots"));
    var expected = List.of(CpmVanillaRoot.values());
    var seenRoots = new HashSet<CpmVanillaRoot>();
    for (int i = 0; i < Math.min(expected.size(), p.roots().size()); i++) {
      var r = p.roots().get(i);
      if (r.root() != expected.get(i)) bag = bag.add(failure("root order is not canonical"));
      if (!seenRoots.add(r.root())) bag = bag.add(failure("duplicate vanilla root"));
      if (r.show() || !r.showInEditor() || r.locked() || r.disableVanillaAnim())
        bag = bag.add(failure("root flags violate T300 policy"));
      if (r.transform() == null
          || !finite(r.transform().position())
          || !finite(r.transform().rotationDegrees())
          || !finite(r.transform().scale())) bag = bag.add(failure("root transform is invalid"));
      if (r.root() != CpmVanillaRoot.BODY && !r.children().isEmpty())
        bag = bag.add(failure("non-BODY root has children"));
      if (r.root() != CpmVanillaRoot.BODY && r.origin().kind() != CpmNodeOriginKind.SYNTHETIC_ROOT)
        bag = bag.add(failure("non-BODY root origin is not synthetic"));
      bag =
          bag.addAll(
              check(
                  r.children(),
                  new HashSet<>(),
                  Collections.newSetFromMap(new IdentityHashMap<>())));
    }
    return bag;
  }

  private DiagnosticBag check(
      List<CpmLogicalElementV1> nodes, Set<String> keys, Set<CpmLogicalElementV1> identity) {
    DiagnosticBag bag = new DiagnosticBag();
    for (var n : nodes) {
      if (n == null || !identity.add(n)) {
        bag = bag.add(failure("node is null or reused"));
        continue;
      }
      if (n.key() == null || n.key().value().startsWith("root:") || !keys.add(n.key().value()))
        bag = bag.add(failure("node key is invalid or duplicated"));
      if (n.name() == null || n.transform() == null || n.origin() == null)
        bag = bag.add(failure("node required field is null"));
      else if (!finite(n.transform().position())
          || !finite(n.transform().rotationDegrees())
          || !finite(n.transform().scale())) bag = bag.add(failure("node transform is not finite"));
      if (n.kind() == CpmNodeKind.BONE
          && (n.cube() != null || n.origin().kind() != CpmNodeOriginKind.SOURCE_BONE))
        bag = bag.add(failure("BONE invariant violated"));
      if (n.kind() == CpmNodeKind.CUBE
          && (n.cube() == null
              || !n.children().isEmpty()
              || n.origin().kind() != CpmNodeOriginKind.SOURCE_CUBE))
        bag = bag.add(failure("CUBE invariant violated"));
      if (n.kind() == CpmNodeKind.CUBE_ROTATION_HELPER
          && (n.cube() != null
              || n.children().size() != 1
              || n.origin().kind() != CpmNodeOriginKind.SYNTHETIC_HELPER
              || n.children().get(0).kind() != CpmNodeKind.CUBE))
        bag = bag.add(failure("helper invariant violated"));
      if (n.cube() != null) {
        var c = n.cube();
        if (!finite(c.offset())
            || !finite(c.size())
            || !finite(c.renderScale())
            || !finite(c.meshScale())
            || c.textureSize() <= 0
            || !Double.isFinite(c.mcScale())
            || c.size().x() < 0
            || c.size().y() < 0
            || c.size().z() < 0
            || c.uv() == null) bag = bag.add(failure("cube invariant violated"));
      }
      bag = bag.addAll(check(n.children(), keys, identity));
    }
    return bag;
  }

  private static boolean finite(Vec3d v) {
    return v != null && v.isFinite();
  }

  private static CpmLogicalElementV1 find(CpmLogicalProjectV1 p, CpmNodeKey key) {
    for (var r : p.roots()) {
      var f = find(r.children(), key);
      if (f != null) return f;
    }
    return null;
  }

  private static CpmLogicalElementV1 find(List<CpmLogicalElementV1> ns, CpmNodeKey key) {
    for (var n : ns) {
      if (n.key().equals(key)) return n;
      var f = find(n.children(), key);
      if (f != null) return f;
    }
    return null;
  }

  private static Diagnostic failure(String message) {
    return Diagnostic.of(
        Severity.ERROR, DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED), message);
  }
}
