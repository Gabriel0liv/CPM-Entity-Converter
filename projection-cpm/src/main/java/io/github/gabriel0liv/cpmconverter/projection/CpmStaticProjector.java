package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.config.SemanticRigMap;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;

/** T300 projection boundary: validated ModelIR to an in-memory CPM V1 logical graph. */
public final class CpmStaticProjector {
  public Result<CpmStaticProjection> project(ModelIR model, SemanticRigMap mapping) {
    if (model == null || mapping == null)
      return Result.failure(
          error(DiagnosticCodes.CPM_VALIDATION_FAILED, "model and mapping are required", Map.of()));
    DiagnosticBag bag = new ModelIrValidator().validate(model);
    if (bag.hasErrors()) return Result.failure(bag);
    String strategy = mapping.rootStrategy() == null ? "single_anchor" : mapping.rootStrategy();
    if (!strategy.equals("single_anchor"))
      return Result.failure(
          error(
              DiagnosticCodes.CPM_INVALID_ROOT,
              "root strategy is not supported in T300",
              Map.of("strategy", strategy, "deferredTo", "ADR-005/T500")));
    BoneId anchor = mapping.rootRoles() == null ? null : mapping.rootRoles().roles().get("body");
    if (anchor == null || model.roots().size() != 1 || !model.roots().contains(anchor))
      return Result.failure(
          error(
              DiagnosticCodes.CPM_INVALID_ROOT,
              "single_anchor requires body as the sole ModelIR root",
              Map.of(
                  "role",
                  "body",
                  "boneId",
                  anchor == null ? "null" : anchor.value(),
                  "rootCount",
                  Integer.toString(model.roots().size()),
                  "strategy",
                  strategy)));
    BoneIR anchorBone =
        model.bones().stream().filter(b -> b.id().equals(anchor)).findFirst().orElse(null);
    if (anchorBone == null)
      return Result.failure(
          error(DiagnosticCodes.CPM_INVALID_ROOT, "body bone is missing", Map.of("role", "body")));
    if (!identity(anchorBone.bind().scale()))
      return Result.failure(
          error(
              DiagnosticCodes.CPM_INVALID_ROOT,
              "anchor scale is not identity",
              Map.of("field", "anchor.scale", "deferredTo", "T501")));
    if (model.textures().size() != 1)
      return Result.failure(
          error(
              DiagnosticCodes.IR_INVALID_VALUE,
              "T300 requires exactly one texture",
              Map.of(
                  "field",
                  "textures",
                  "observed",
                  Integer.toString(model.textures().size()),
                  "expected",
                  "1")));
    CpmUvProjector uvProjector = new CpmUvProjector();
    for (var sourceBone : model.bones()) {
      for (var sourceCube : sourceBone.cubes()) {
        Result<CpmUvV1> uv = uvProjector.project(sourceCube);
        if (!uv.success()) return Result.failure(uv.diagnostics());
      }
    }
    String skin = mapping.skin() == null ? "default" : mapping.skin().toLowerCase(Locale.ROOT);
    if (!skin.equals("default") && !skin.equals("slim"))
      return Result.failure(
          error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "unsupported skin",
              Map.of("field", "skin", "value", skin, "supported", "default,slim")));
    if (mapping.modelScale() != null && Math.abs(mapping.modelScale() - 1) > 1e-12
        || mapping.verticalOffset() != null && Math.abs(mapping.verticalOffset()) > 1e-12)
      return Result.failure(
          error(
              DiagnosticCodes.CPM_VALIDATION_FAILED,
              "model scale or vertical offset is deferred",
              Map.of("deferredTo", "T501")));
    Map<BoneId, BoneIR> byId = new LinkedHashMap<>();
    for (var b : model.bones()) byId.put(b.id(), b);
    Result<Map<BoneId, Vec3d>> pivotResult =
        new AuthoredPivotResolver(model.bones(), anchor).resolve();
    if (!pivotResult.success()) return Result.failure(pivotResult.diagnostics());
    Map<BoneId, Vec3d> pivots = pivotResult.value();
    List<CpmLogicalRootV1> roots = new ArrayList<>();
    List<Diagnostic> projectionDiagnostics = new ArrayList<>();
    Map<BoneId, CpmTargetRef> boneTargets = new LinkedHashMap<>();
    Map<CubeId, CpmTargetRef> cubeTargets = new LinkedHashMap<>();
    Map<CubeId, CpmNodeKey> helperTargets = new LinkedHashMap<>();
    for (var root : CpmVanillaRoot.values()) {
      List<CpmLogicalElementV1> children =
          root == CpmVanillaRoot.BODY
              ? children(
                  anchorBone,
                  pivots,
                  byId,
                  boneTargets,
                  cubeTargets,
                  helperTargets,
                  projectionDiagnostics)
              : List.of();
      CpmTransformV1 t =
          root == CpmVanillaRoot.BODY
              ? transform(anchorBone.bind())
              : new CpmTransformV1(Vec3d.ZERO, Vec3d.ZERO, new Vec3d(1, 1, 1));
      roots.add(
          new CpmLogicalRootV1(
              root,
              t,
              false,
              true,
              false,
              false,
              children,
              root == CpmVanillaRoot.BODY
                  ? CpmNodeOrigin.sourceBone(anchor, anchorBone.provenance())
                  : CpmNodeOrigin.syntheticRoot(root)));
    }
    boneTargets.put(anchor, CpmTargetRef.root(CpmVanillaRoot.BODY));
    Map<BoneId, CpmTargetRef> orderedBoneTargets = new LinkedHashMap<>();
    Map<CubeId, CpmTargetRef> orderedCubeTargets = new LinkedHashMap<>();
    Map<CubeId, CpmNodeKey> orderedHelperTargets = new LinkedHashMap<>();
    for (var sourceBone : model.bones()) {
      var target = boneTargets.get(sourceBone.id());
      if (target == null)
        return Result.failure(
            error(
                DiagnosticCodes.CPM_VALIDATION_FAILED,
                "missing bone projection target",
                Map.of("boneId", sourceBone.id().value())));
      orderedBoneTargets.put(sourceBone.id(), target);
      for (var sourceCube : sourceBone.cubes()) {
        var cubeTarget = cubeTargets.get(sourceCube.id());
        if (cubeTarget == null)
          return Result.failure(
              error(
                  DiagnosticCodes.CPM_VALIDATION_FAILED,
                  "missing cube projection target",
                  Map.of("cubeId", sourceCube.id().value())));
        orderedCubeTargets.put(sourceCube.id(), cubeTarget);
        if (helperTargets.containsKey(sourceCube.id()))
          orderedHelperTargets.put(sourceCube.id(), helperTargets.get(sourceCube.id()));
      }
    }
    var sourceTexture = model.textures().get(0);
    CpmLogicalProjectV1 project =
        new CpmLogicalProjectV1(
            1,
            skin,
            new CpmLogicalTextureV1(
                sourceTexture.path(), sourceTexture.width(), sourceTexture.height(), skin, false),
            roots);
    CpmStaticProjection out =
        new CpmStaticProjection(
            project,
            new CpmProjectionIndex(orderedBoneTargets, orderedCubeTargets, orderedHelperTargets));
    bag = bag.addAll(new DiagnosticBag(projectionDiagnostics));
    DiagnosticBag validation = new CpmLogicalProjectionValidator().validate(out, model, anchor);
    bag = bag.addAll(validation);
    if (bag.hasErrors()) return Result.failure(bag);
    return Result.success(out, bag);
  }

  private List<CpmLogicalElementV1> children(
      BoneIR bone,
      Map<BoneId, Vec3d> pivots,
      Map<BoneId, BoneIR> byId,
      Map<BoneId, CpmTargetRef> bt,
      Map<CubeId, CpmTargetRef> ct,
      Map<CubeId, CpmNodeKey> ht,
      List<Diagnostic> projectionDiagnostics) {
    List<CpmLogicalElementV1> out = new ArrayList<>();
    int i = 0;
    for (var c : bone.cubes()) {
      boolean rotated = Math.abs(Math.abs(c.rotation().normalized().w()) - 1) > 1e-12;
      CpmNodeKey ck = new CpmNodeKey("cube:" + c.id().value());
      CpmLogicalCubeV1 cube =
          new CpmLogicalCubeV1(
              rotated ? c.origin().subtract(c.pivot()) : c.origin().subtract(pivots.get(bone.id())),
              c.size(),
              new Vec3d(1, 1, 1),
              new Vec3d(1, 1, 1),
              true,
              1,
              new CpmUvProjector().project(c).value(),
              "ffffff",
              c.mirror(),
              c.inflate(),
              true,
              false,
              false,
              false);
      CpmLogicalElementV1 ce =
          new CpmLogicalElementV1(
              ck,
              CpmNodeKind.CUBE,
              bone.name() + "_cube_" + i,
              new CpmTransformV1(Vec3d.ZERO, Vec3d.ZERO, new Vec3d(1, 1, 1)),
              cube,
              List.of(),
              CpmNodeOrigin.sourceCube(bone.id(), c.id(), c.provenance()));
      ct.put(c.id(), CpmTargetRef.element(ck));
      if (rotated) {
        CpmNodeKey hk = new CpmNodeKey("helper:" + c.id().value());
        CpmLogicalElementV1 helper =
            new CpmLogicalElementV1(
                hk,
                CpmNodeKind.CUBE_ROTATION_HELPER,
                bone.name() + "_cube_" + i + "_pivot",
                new CpmTransformV1(
                    c.pivot().subtract(pivots.get(bone.id())),
                    QuaternionToEulerZYX.decompose(c.rotation()),
                    new Vec3d(1, 1, 1)),
                null,
                List.of(ce),
                CpmNodeOrigin.syntheticHelper(bone.id(), c.id(), c.provenance()));
        ht.put(c.id(), hk);
        projectionDiagnostics.add(
            new Diagnostic(
                Severity.INFO,
                DiagnosticCode.fromCatalog(DiagnosticCodes.GEO_CUBE_HELPER_SYNTHESIZED),
                c.provenance(),
                "Cube rotation helper synthesized",
                "Review the helper node in the logical projection",
                bone.name(),
                null,
                new TreeMap<>(
                    Map.of(
                        "boneId", bone.id().value(),
                        "cubeId", c.id().value(),
                        "helperKey", hk.value()))));
        out.add(helper);
      } else out.add(ce);
      i++;
    }
    for (var child : bone.children()) {
      BoneIR b = byId.get(child);
      if (b == null) continue;
      CpmNodeKey k = new CpmNodeKey("bone:" + b.id().value());
      bt.put(b.id(), CpmTargetRef.element(k));
      out.add(
          new CpmLogicalElementV1(
              k,
              CpmNodeKind.BONE,
              b.name(),
              transform(b.bind()),
              null,
              children(b, pivots, byId, bt, ct, ht, projectionDiagnostics),
              CpmNodeOrigin.sourceBone(b.id(), b.provenance())));
    }
    return List.copyOf(out);
  }

  private static CpmTransformV1 transform(Transform t) {
    return new CpmTransformV1(
        t.translation(), QuaternionToEulerZYX.decompose(t.rotation()), t.scale());
  }

  private static boolean identity(Vec3d s) {
    return Math.abs(s.x() - 1) < 1e-12
        && Math.abs(s.y() - 1) < 1e-12
        && Math.abs(s.z() - 1) < 1e-12;
  }

  private static Diagnostic error(String code, String msg, Map<String, String> ctx) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        null,
        msg,
        "Fix the projection input",
        null,
        null,
        new TreeMap<>(ctx));
  }
}
