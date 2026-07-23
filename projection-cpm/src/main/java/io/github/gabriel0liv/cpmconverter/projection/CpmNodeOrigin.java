package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.ir.*;

public record CpmNodeOrigin(
    CpmNodeOriginKind kind,
    CpmVanillaRoot root,
    BoneId boneId,
    CubeId cubeId,
    SourceLocation source) {
  public CpmNodeOrigin {
    if (kind == null) throw new IllegalArgumentException("kind");
    if (kind == CpmNodeOriginKind.SYNTHETIC_ROOT) {
      if (root == null || boneId != null || cubeId != null)
        throw new IllegalArgumentException("root origin");
    } else if (boneId == null
        || source == null
        || root != null
        || (kind == CpmNodeOriginKind.SOURCE_BONE && cubeId != null)
        || (kind != CpmNodeOriginKind.SOURCE_BONE && cubeId == null))
      throw new IllegalArgumentException("source origin");
  }

  public static CpmNodeOrigin syntheticRoot(CpmVanillaRoot r) {
    return new CpmNodeOrigin(CpmNodeOriginKind.SYNTHETIC_ROOT, r, null, null, null);
  }

  public static CpmNodeOrigin sourceBone(BoneId id, SourceLocation s) {
    return new CpmNodeOrigin(CpmNodeOriginKind.SOURCE_BONE, null, id, null, s);
  }

  public static CpmNodeOrigin sourceCube(BoneId b, CubeId c, SourceLocation s) {
    return new CpmNodeOrigin(CpmNodeOriginKind.SOURCE_CUBE, null, b, c, s);
  }

  public static CpmNodeOrigin syntheticHelper(BoneId b, CubeId c, SourceLocation s) {
    return new CpmNodeOrigin(CpmNodeOriginKind.SYNTHETIC_HELPER, null, b, c, s);
  }
}
