package io.github.gabriel0liv.cpmconverter.geckolib4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIrValidator;
import io.github.gabriel0liv.cpmconverter.ir.SourceDescriptor;
import io.github.gabriel0liv.cpmconverter.ir.UvIR;
import io.github.gabriel0liv.cpmconverter.math.CoordinateBoundary;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict offline parser for the supported GeckoLib 4.4.9 geometry 1.12.0 subset. */
public final class GeckoGeometryParser {
  private static final String SUPPORTED_FORMAT = "1.12.0";
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  private final ObjectMapper json = new ObjectMapper();

  public Result<ModelIR> parse(Path path) {
    return parse(path, null);
  }

  public Result<ModelIR> parse(Path path, String requestedGeometryId) {
    try {
      if (path == null) return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "geometry path is null");
      JsonNode root = json.readTree(Files.readString(path));
      if (root == null || !root.isObject()) {
        return failure(DiagnosticCodes.INPUT_PARSE_ERROR, "geometry root must be an object");
      }

      JsonNode formatNode = root.get("format_version");
      String format = formatNode != null && formatNode.isTextual() ? formatNode.textValue() : null;
      if (!SUPPORTED_FORMAT.equals(format)) {
        return failure(
            DiagnosticCodes.INPUT_UNSUPPORTED_VERSION,
            "expected Gecko geometry format 1.12.0, got " + String.valueOf(format));
      }

      JsonNode geometries = root.get("minecraft:geometry");
      if (geometries == null || !geometries.isArray()) {
        return failure(DiagnosticCodes.GEO_MODEL_NOT_FOUND, "minecraft:geometry array is missing");
      }

      Selection selection = selectGeometry(geometries, requestedGeometryId);
      JsonNode geometry = selection.geometry();
      String geometryId = selection.identifier();
      JsonNode bonesNode = geometry.get("bones");
      if (bonesNode == null || !bonesNode.isArray()) {
        return failure(DiagnosticCodes.GEO_INVALID_VALUE, "geometry bones must be an array");
      }

      List<RawBone> rawBones = readBones(bonesNode);
      Map<String, RawBone> bonesByName = indexBones(rawBones);
      validateParents(rawBones, bonesByName);
      validateAcyclic(rawBones, bonesByName);

      Map<String, List<BoneId>> children = new LinkedHashMap<>();
      for (RawBone bone : rawBones) children.put(bone.name(), new ArrayList<>());
      for (RawBone bone : rawBones) {
        if (bone.parent() != null) children.get(bone.parent()).add(new BoneId(bone.name()));
      }

      List<BoneIR> bones = new ArrayList<>();
      List<BoneId> roots = new ArrayList<>();
      for (RawBone bone : rawBones) {
        BoneId id = new BoneId(bone.name());
        RawBone parent = bone.parent() == null ? null : bonesByName.get(bone.parent());
        if (parent == null) roots.add(id);
        Vec3d localPivot = parent == null ? bone.pivot() : bone.pivot().subtract(parent.pivot());
        Transform bind =
            new Transform(
                CoordinateBoundary.geckoToCpmPosition(localPivot),
                rotation(bone.rotationDegrees()),
                ONE);
        List<CubeIR> cubes = readCubes(bone, id);
        bones.add(
            new BoneIR(
                id,
                bone.name(),
                parent == null ? null : new BoneId(parent.name()),
                children.get(bone.name()),
                bind,
                cubes,
                provenance(path, bone.pointer())));
      }

      ModelIR model =
          new ModelIR(
              new SourceDescriptor(sourcePath(path), SUPPORTED_FORMAT),
              new GeometryId(geometryId),
              bones,
              roots,
              List.of(),
              List.of(),
              List.of());
      DiagnosticBag irDiagnostics = new ModelIrValidator().validate(model);
      if (irDiagnostics.hasErrors()) return Result.failure(irDiagnostics);
      return Result.success(model, irDiagnostics);
    } catch (GeometryParseException exception) {
      return failure(exception.code(), exception.getMessage());
    } catch (Exception exception) {
      return failure(
          DiagnosticCodes.INPUT_PARSE_ERROR,
          exception.getMessage() == null ? "cannot parse geometry" : exception.getMessage());
    }
  }

  private Selection selectGeometry(JsonNode geometries, String requestedId)
      throws GeometryParseException {
    if (requestedId == null || requestedId.isBlank()) {
      if (geometries.size() == 0) {
        throw error(DiagnosticCodes.GEO_MODEL_NOT_FOUND, "geometry file contains no models");
      }
      if (geometries.size() != 1) {
        throw error(
            DiagnosticCodes.GEO_MULTIPLE_MODELS,
            "geometry file contains multiple models; an exact identifier is required");
      }
      JsonNode geometry = geometries.get(0);
      return new Selection(geometry, geometryIdentifier(geometry));
    }

    JsonNode selected = null;
    for (JsonNode geometry : geometries) {
      String identifier = geometryIdentifier(geometry);
      if (requestedId.equals(identifier)) {
        if (selected != null) {
          throw error(
              DiagnosticCodes.GEO_MULTIPLE_MODELS,
              "multiple geometry entries use identifier " + requestedId);
        }
        selected = geometry;
      }
    }
    if (selected == null) {
      throw error(
          DiagnosticCodes.GEO_MODEL_NOT_FOUND, "geometry identifier not found: " + requestedId);
    }
    return new Selection(selected, requestedId);
  }

  private String geometryIdentifier(JsonNode geometry) throws GeometryParseException {
    JsonNode description = geometry == null ? null : geometry.get("description");
    JsonNode identifier = description == null ? null : description.get("identifier");
    if (identifier == null || !identifier.isTextual() || identifier.textValue().isBlank()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "geometry identifier is required");
    }
    return identifier.textValue();
  }

  private List<RawBone> readBones(JsonNode bonesNode) throws GeometryParseException {
    List<RawBone> bones = new ArrayList<>();
    for (int index = 0; index < bonesNode.size(); index++) {
      JsonNode node = bonesNode.get(index);
      if (node == null || !node.isObject()) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "bone must be an object at index " + index);
      }
      String pointer = "/bones/" + index;
      String name = requiredText(node, "name", pointer);
      String parent = optionalText(node, "parent", pointer);
      Vec3d pivot = optionalVec3(node.get("pivot"), Vec3d.ZERO, pointer + "/pivot");
      Vec3d rotation = optionalVec3(node.get("rotation"), Vec3d.ZERO, pointer + "/rotation");
      Double inflate = optionalDouble(node.get("inflate"), pointer + "/inflate");
      if (node.has("poly_mesh") && !node.get("poly_mesh").isNull()) {
        throw error(DiagnosticCodes.GEO_MESH_UNSUPPORTED, "poly_mesh is outside the MVP subset");
      }
      bones.add(new RawBone(name, parent, pivot, rotation, inflate, node.get("cubes"), pointer));
    }
    return List.copyOf(bones);
  }

  private Map<String, RawBone> indexBones(List<RawBone> bones) throws GeometryParseException {
    Map<String, RawBone> indexed = new LinkedHashMap<>();
    for (RawBone bone : bones) {
      if (indexed.putIfAbsent(bone.name(), bone) != null) {
        throw error(
            DiagnosticCodes.GEO_DUPLICATE_BONE_NAME, "duplicate bone name: " + bone.name());
      }
    }
    return indexed;
  }

  private void validateParents(List<RawBone> bones, Map<String, RawBone> indexed)
      throws GeometryParseException {
    for (RawBone bone : bones) {
      if (bone.parent() != null && !indexed.containsKey(bone.parent())) {
        throw error(
            DiagnosticCodes.GEO_PARENT_NOT_FOUND,
            "parent " + bone.parent() + " not found for bone " + bone.name());
      }
    }
  }

  private void validateAcyclic(List<RawBone> bones, Map<String, RawBone> indexed)
      throws GeometryParseException {
    for (RawBone bone : bones) {
      Set<String> visited = new HashSet<>();
      RawBone current = bone;
      while (current != null) {
        if (!visited.add(current.name())) {
          throw error(
              DiagnosticCodes.GEO_HIERARCHY_CYCLE,
              "bone hierarchy cycle includes " + current.name());
        }
        current = current.parent() == null ? null : indexed.get(current.parent());
      }
    }
  }

  private List<CubeIR> readCubes(RawBone bone, BoneId boneId) throws GeometryParseException {
    JsonNode cubesNode = bone.cubes();
    if (cubesNode == null || cubesNode.isNull()) return List.of();
    if (!cubesNode.isArray()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "cubes must be an array for bone " + bone.name());
    }

    List<CubeIR> cubes = new ArrayList<>();
    for (int index = 0; index < cubesNode.size(); index++) {
      JsonNode cube = cubesNode.get(index);
      String pointer = bone.pointer() + "/cubes/" + index;
      if (cube == null || !cube.isObject()) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "cube must be an object at " + pointer);
      }
      Vec3d origin = requiredVec3(cube.get("origin"), pointer + "/origin");
      Vec3d size = requiredVec3(cube.get("size"), pointer + "/size");
      if (size.x() < 0 || size.y() < 0 || size.z() < 0) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "cube size must not be negative at " + pointer);
      }
      boolean hasPivot = cube.has("pivot") && !cube.get("pivot").isNull();
      Vec3d effectivePivot =
          hasPivot ? requiredVec3(cube.get("pivot"), pointer + "/pivot") : bone.pivot();
      Vec3d localPivot =
          hasPivot
              ? CoordinateBoundary.geckoToCpmPosition(effectivePivot.subtract(bone.pivot()))
              : Vec3d.ZERO;
      Vec3d localOrigin =
          new Vec3d(
              effectivePivot.x() - (origin.x() + size.x()),
              effectivePivot.y() - (origin.y() + size.y()),
              origin.z() - effectivePivot.z());
      Vec3d rotationDegrees =
          optionalVec3(cube.get("rotation"), Vec3d.ZERO, pointer + "/rotation");
      Double cubeInflate = optionalDouble(cube.get("inflate"), pointer + "/inflate");
      double inflate = cubeInflate != null ? cubeInflate : bone.inflate() == null ? 0 : bone.inflate();
      boolean mirror = optionalBoolean(cube.get("mirror"), false, pointer + "/mirror");
      UvIR uv = readUv(cube.get("uv"), pointer + "/uv");
      cubes.add(
          new CubeIR(
              new CubeId(bone.name() + "#cube-" + index),
              boneId,
              localOrigin,
              size,
              localPivot,
              rotation(rotationDegrees),
              inflate,
              mirror,
              uv,
              pointer));
    }
    return List.copyOf(cubes);
  }

  private UvIR readUv(JsonNode uv, String pointer) throws GeometryParseException {
    if (uv == null || uv.isNull()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "cube uv is required at " + pointer);
    }
    if (uv.isArray()) {
      if (uv.size() != 2 || !uv.get(0).isIntegralNumber() || !uv.get(1).isIntegralNumber()) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "box uv must contain two integers at " + pointer);
      }
      int u = uv.get(0).intValue();
      int v = uv.get(1).intValue();
      if (u < 0 || v < 0) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "box uv must not be negative at " + pointer);
      }
      return new BoxUvIR(u, v);
    }
    if (uv.isObject()) {
      throw error(
          DiagnosticCodes.GEO_UV_UNSUPPORTED,
          "per-face UV parsing is deferred to T201 at " + pointer);
    }
    throw error(DiagnosticCodes.GEO_INVALID_VALUE, "invalid cube uv at " + pointer);
  }

  private Quatd rotation(Vec3d geckoDegrees) {
    Vec3d cpmDegrees = CoordinateBoundary.geckoToCpmRotationDegrees(geckoDegrees);
    return Quatd.fromEulerZYX(
        Math.toRadians(cpmDegrees.x()),
        Math.toRadians(cpmDegrees.y()),
        Math.toRadians(cpmDegrees.z()));
  }

  private String requiredText(JsonNode object, String field, String pointer)
      throws GeometryParseException {
    JsonNode node = object.get(field);
    if (node == null || !node.isTextual() || node.textValue().isBlank()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, field + " must be non-empty at " + pointer);
    }
    return node.textValue();
  }

  private String optionalText(JsonNode object, String field, String pointer)
      throws GeometryParseException {
    JsonNode node = object.get(field);
    if (node == null || node.isNull()) return null;
    if (!node.isTextual() || node.textValue().isBlank()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, field + " must be non-empty at " + pointer);
    }
    return node.textValue();
  }

  private Vec3d requiredVec3(JsonNode node, String pointer) throws GeometryParseException {
    if (node == null || node.isNull()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "vec3 is required at " + pointer);
    }
    return vec3(node, pointer);
  }

  private Vec3d optionalVec3(JsonNode node, Vec3d defaultValue, String pointer)
      throws GeometryParseException {
    return node == null || node.isNull() ? defaultValue : vec3(node, pointer);
  }

  private Vec3d vec3(JsonNode node, String pointer) throws GeometryParseException {
    if (!node.isArray() || node.size() != 3) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "expected three numbers at " + pointer);
    }
    double[] values = new double[3];
    for (int index = 0; index < 3; index++) {
      JsonNode value = node.get(index);
      if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
        throw error(DiagnosticCodes.GEO_INVALID_VALUE, "expected finite number at " + pointer);
      }
      values[index] = value.doubleValue();
    }
    return new Vec3d(values[0], values[1], values[2]);
  }

  private Double optionalDouble(JsonNode node, String pointer) throws GeometryParseException {
    if (node == null || node.isNull()) return null;
    if (!node.isNumber() || !Double.isFinite(node.doubleValue())) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "expected finite number at " + pointer);
    }
    return node.doubleValue();
  }

  private boolean optionalBoolean(JsonNode node, boolean defaultValue, String pointer)
      throws GeometryParseException {
    if (node == null || node.isNull()) return defaultValue;
    if (!node.isBoolean()) {
      throw error(DiagnosticCodes.GEO_INVALID_VALUE, "expected boolean at " + pointer);
    }
    return node.booleanValue();
  }

  private String sourcePath(Path path) {
    Path fileName = path.getFileName();
    return fileName == null ? "geometry.geo.json" : fileName.toString();
  }

  private String provenance(Path path, String pointer) {
    return sourcePath(path) + "#" + pointer;
  }

  private GeometryParseException error(String code, String message) {
    return new GeometryParseException(code, message);
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }

  private record Selection(JsonNode geometry, String identifier) {}

  private record RawBone(
      String name,
      String parent,
      Vec3d pivot,
      Vec3d rotationDegrees,
      Double inflate,
      JsonNode cubes,
      String pointer) {}

  private static final class GeometryParseException extends Exception {
    private final String code;

    private GeometryParseException(String code, String message) {
      super(message);
      this.code = code;
    }

    private String code() {
      return code;
    }
  }
}
