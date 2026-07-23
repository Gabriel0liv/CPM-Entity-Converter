package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.*;

public final class AuthoredPivotResolver {
  private final Map<BoneId, BoneIR> bones;
  private final BoneId anchor;
  private final Map<BoneId, Vec3d> resolved = new LinkedHashMap<>();
  private final Set<BoneId> visiting = new HashSet<>();

  public AuthoredPivotResolver(List<BoneIR> bones, BoneId anchor) {
    this.bones = new LinkedHashMap<>();
    for (var b : bones) this.bones.put(b.id(), b);
    this.anchor = anchor;
  }

  public Result<Map<BoneId, Vec3d>> resolve() {
    try {
      for (var id : bones.keySet()) resolve(id);
      return Result.success(Collections.unmodifiableMap(new LinkedHashMap<>(resolved)));
    } catch (IllegalStateException e) {
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_INVALID_ROOT),
              e.getMessage()));
    }
  }

  private Vec3d resolve(BoneId id) {
    if (resolved.containsKey(id)) return resolved.get(id);
    if (!visiting.add(id)) throw new IllegalStateException("cycle while resolving authored pivots");
    BoneIR b = bones.get(id);
    if (b == null) throw new IllegalStateException("bone missing while resolving authored pivots");
    Vec3d p =
        id.equals(anchor)
            ? b.bind().translation()
            : resolve(b.parent()).add(b.bind().translation());
    visiting.remove(id);
    resolved.put(id, p);
    return p;
  }
}
