package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Stateless defensive validation for the T200 parsed geometry boundary. */
public final class ParsedGeometryValidator {
  public Result<ParsedGeometry> validate(ParsedGeometry geometry) {
    if (geometry == null) {
      return Result.failure(
          error(
              DiagnosticCodes.INPUT_PARSE_ERROR,
              null,
              "geometry is null",
              "Provide geometry input",
              Map.of()));
    }
    DiagnosticBag diagnostics = new DiagnosticBag();
    if (geometry.source() == null
        || geometry.geometryId() == null
        || geometry.bones() == null
        || geometry.roots() == null) {
      diagnostics =
          diagnostics.add(
              error(
                  DiagnosticCodes.IR_INVALID_VALUE,
                  source(geometry),
                  "geometry boundary contains null fields",
                  "Provide all geometry fields",
                  Map.of("component", "document")));
      return Result.failure(diagnostics);
    }
    Map<String, ParsedBone> byId = new HashMap<>();
    Map<String, ParsedBone> byName = new HashMap<>();
    Set<String> rootIds = new HashSet<>();
    for (ParsedBone bone : geometry.bones()) {
      if (bone == null
          || bone.id() == null
          || bone.sourceName() == null
          || bone.sourceName().isBlank()
          || bone.bindLocal() == null
          || bone.source() == null
          || bone.source().jsonPointer() == null
          || bone.source().jsonPointer().isBlank()) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_INVALID_VALUE,
                    bone == null ? source(geometry) : bone.source(),
                    "bone provenance or identity is invalid",
                    "Provide a non-empty id, name, pointer and bind transform",
                    Map.of("component", "bone")));
        continue;
      }
      if (byId.putIfAbsent(bone.id().value(), bone) != null)
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_DUPLICATE_BONE_ID,
                    bone.source(),
                    "duplicate bone id",
                    "Use a unique deterministic BoneId",
                    Map.of("boneId", bone.id().value())));
      if (byName.putIfAbsent(bone.sourceName(), bone) != null)
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.GEO_DUPLICATE_BONE_NAME,
                    bone.source(),
                    "duplicate bone source name",
                    "Rename the source bone",
                    Map.of("bone", bone.sourceName())));
      if (!finite(bone.bindLocal()))
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_INVALID_VALUE,
                    bone.source(),
                    "bone bind transform is non-finite",
                    "Use finite translation, rotation and scale",
                    Map.of("boneId", bone.id().value())));
      if (bone.parent() == null) rootIds.add(bone.id().value());
    }
    Set<String> declaredRoots = new HashSet<>();
    for (var root : geometry.roots()) {
      if (root == null || root.value() == null) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_MISSING,
                    source(geometry),
                    "root id is null",
                    "Reference an existing root bone",
                    Map.of("rootId", "null")));
      } else if (!declaredRoots.add(root.value())) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_DUPLICATE,
                    source(geometry),
                    "duplicate root id",
                    "Declare each root once",
                    Map.of("rootId", root.value())));
      } else if (!byId.containsKey(root.value())) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_MISSING,
                    source(geometry),
                    "root bone is missing",
                    "Reference an existing bone",
                    Map.of("rootId", root.value())));
      } else if (byId.get(root.value()).parent() != null) {
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_ROOT_PARENT,
                    byId.get(root.value()).source(),
                    "root has a parent",
                    "Remove the parent or remove the root designation",
                    Map.of("rootId", root.value())));
      }
    }
    if (!declaredRoots.equals(rootIds))
      diagnostics =
          diagnostics.add(
              error(
                  DiagnosticCodes.IR_PARENT_CHILD_MISMATCH,
                  source(geometry),
                  "roots do not match parentless bones",
                  "Keep roots synchronized with parent references",
                  Map.of("component", "roots")));
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();
    Set<String> reachable = new HashSet<>();
    for (var root : geometry.roots()) {
      if (root != null && byId.containsKey(root.value())) {
        markReachable(root.value(), byId, reachable, new HashSet<>());
      }
    }
    for (ParsedBone bone : geometry.bones()) {
      if (bone == null || bone.id() == null) continue;
      if (bone.parent() != null && !byId.containsKey(bone.parent().value()))
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.GEO_PARENT_NOT_FOUND,
                    bone.source(),
                    "parent bone is missing",
                    "Declare the referenced parent",
                    Map.of("boneId", bone.id().value(), "parentId", bone.parent().value())));
      if (cycle(bone.id().value(), byId, visiting, visited))
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.GEO_HIERARCHY_CYCLE,
                    bone.source(),
                    "bone hierarchy contains a cycle",
                    "Break the parent cycle",
                    Map.of("boneId", bone.id().value())));
      for (var child : bone.children()) {
        if (child == null || child.value() == null) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CHILD_MISSING,
                      bone.source(),
                      "child id is null",
                      "Reference an existing child",
                      Map.of("boneId", bone.id().value())));
          continue;
        }
        long count =
            bone.children().stream()
                .filter(candidate -> candidate != null && child.value().equals(candidate.value()))
                .count();
        if (count > 1)
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CHILD_DUPLICATE,
                      bone.source(),
                      "duplicate child id",
                      "List each child once",
                      Map.of("boneId", bone.id().value(), "childId", child.value())));
        ParsedBone childBone = byId.get(child.value());
        if (childBone == null)
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CHILD_MISSING,
                      bone.source(),
                      "child bone is missing",
                      "Declare the referenced child",
                      Map.of("childId", child.value())));
        else if (childBone.parent() == null || !bone.id().equals(childBone.parent()))
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_PARENT_CHILD_MISMATCH,
                      childBone.source(),
                      "parent and child declarations disagree",
                      "Update both sides of the relationship",
                      Map.of("parentId", bone.id().value(), "childId", child.value())));
      }
    }
    for (ParsedBone bone : geometry.bones()) {
      if (bone != null && bone.id() != null && !reachable.contains(bone.id().value()))
        diagnostics =
            diagnostics.add(
                error(
                    DiagnosticCodes.IR_UNREACHABLE_BONE,
                    bone.source(),
                    "bone is unreachable from roots",
                    "Attach the bone to a declared root",
                    Map.of("boneId", bone.id().value())));
    }
    Set<String> cubeIds = new HashSet<>();
    for (ParsedBone bone : geometry.bones()) {
      if (bone == null || bone.cubes() == null) continue;
      for (ParsedCube cube : bone.cubes()) {
        if (cube == null
            || cube.id() == null
            || cube.boneId() == null
            || cube.source() == null
            || cube.source().jsonPointer() == null
            || cube.origin() == null
            || cube.size() == null
            || cube.pivot() == null
            || cube.rotationDegrees() == null
            || !finite(cube.origin())
            || !finite(cube.size())
            || !finite(cube.pivot())
            || !finite(cube.rotationDegrees())
            || !Double.isFinite(cube.inflate())) {
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_INVALID_VALUE,
                      cube == null ? bone.source() : cube.source(),
                      "cube values or provenance are invalid",
                      "Provide finite vectors and a JSON pointer",
                      Map.of("component", "cube")));
          continue;
        }
        if (!cubeIds.add(cube.id().value()))
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_DUPLICATE_CUBE_ID,
                      cube.source(),
                      "duplicate cube id",
                      "Use a unique deterministic CubeId",
                      Map.of("cubeId", cube.id().value())));
        if (!cube.boneId().equals(bone.id()) || !byId.containsKey(cube.boneId().value()))
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_CUBE_BONE_MISSING,
                      cube.source(),
                      "cube owner is invalid",
                      "Attach the cube to its containing bone",
                      Map.of("cubeId", cube.id().value(), "boneId", cube.boneId().value())));
        if (cube.size().x() < 0 || cube.size().y() < 0 || cube.size().z() < 0)
          diagnostics =
              diagnostics.add(
                  error(
                      DiagnosticCodes.IR_INVALID_VALUE,
                      cube.source(),
                      "cube size is negative",
                      "Use non-negative dimensions",
                      Map.of("cubeId", cube.id().value(), "component", "size")));
      }
    }
    return diagnostics.hasErrors()
        ? Result.failure(diagnostics)
        : Result.success(geometry, diagnostics);
  }

  private static boolean cycle(
      String id, Map<String, ParsedBone> byId, Set<String> visiting, Set<String> visited) {
    if (visited.contains(id)) return false;
    if (!visiting.add(id)) return true;
    ParsedBone bone = byId.get(id);
    boolean result =
        bone != null
            && bone.parent() != null
            && byId.containsKey(bone.parent().value())
            && cycle(bone.parent().value(), byId, visiting, visited);
    visiting.remove(id);
    visited.add(id);
    return result;
  }

  private static void markReachable(
      String id, Map<String, ParsedBone> byId, Set<String> reachable, Set<String> active) {
    if (!reachable.add(id) || !active.add(id)) return;
    ParsedBone bone = byId.get(id);
    if (bone != null) {
      for (var child : bone.children()) {
        if (child != null && byId.containsKey(child.value())) {
          markReachable(child.value(), byId, reachable, active);
        }
      }
    }
    active.remove(id);
  }

  private static boolean finite(Transform transform) {
    return finite(transform.translation())
        && finite(transform.scale())
        && finite(transform.rotation());
  }

  private static boolean finite(Vec3d vector) {
    return vector != null
        && Double.isFinite(vector.x())
        && Double.isFinite(vector.y())
        && Double.isFinite(vector.z());
  }

  private static boolean finite(Quatd quaternion) {
    return quaternion != null
        && Double.isFinite(quaternion.w())
        && Double.isFinite(quaternion.x())
        && Double.isFinite(quaternion.y())
        && Double.isFinite(quaternion.z());
  }

  private static SourceLocation source(ParsedGeometry geometry) {
    return geometry == null || geometry.source() == null
        ? null
        : SourceLocation.of(geometry.source());
  }

  private static Diagnostic error(
      String code,
      SourceLocation location,
      String message,
      String suggestion,
      Map<String, String> context) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        location,
        message,
        suggestion,
        null,
        null,
        new java.util.TreeMap<>(context));
  }
}
