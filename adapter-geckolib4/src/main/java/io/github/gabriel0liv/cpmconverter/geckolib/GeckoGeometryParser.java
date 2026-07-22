package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.ir.FeatureOccurrence;
import io.github.gabriel0liv.cpmconverter.ir.GeometryId;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parses the static Bedrock geometry boundary owned by T200. */
public final class GeckoGeometryParser {
  private static final ObjectMapper JSON = new ObjectMapper();

  public synchronized Result<ParsedGeometry> parse(Path path, GeometryParseRequest request) {
    if (path == null) {
      return failure(
          null, DiagnosticCodes.INPUT_PARSE_ERROR, "geometry path is null", "Provide a path");
    }
    try {
      GeometryParseRequest safe = request == null ? GeometryParseRequest.defaults() : request;
      if (Files.size(path) > safe.limits().maxBytes()) {
        return limitFailure(
            source(path, "/"),
            DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
            "geometry file exceeds maxBytes",
            "Increase maxBytes or provide a smaller file",
            "maxBytes",
            safe.limits().maxBytes(),
            Files.size(path));
      }
      return parse(Files.readAllBytes(path), sourcePath(path), safe);
    } catch (IOException exception) {
      return failure(
          source(path, null),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "unable to read geometry: " + exception.getMessage(),
          "Check that the file is readable");
    }
  }

  public synchronized Result<ParsedGeometry> parse(
      byte[] content, SourcePath source, GeometryParseRequest request) {
    GeometryParseRequest safe = request == null ? GeometryParseRequest.defaults() : request;
    if (content == null || source == null) {
      return failure(
          source == null ? null : SourceLocation.of(source),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "geometry content and source are required",
          "Provide non-null geometry input");
    }
    if (content.length > safe.limits().maxBytes()) {
      return limitFailure(
          location(source, "/"),
          DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
          "geometry file exceeds maxBytes",
          "Increase maxBytes or provide a smaller file",
          "maxBytes",
          safe.limits().maxBytes(),
          content.length);
    }
    try {
      JsonNode root = JSON.readTree(content);
      if (root == null || !root.isObject()) {
        return failure(
            location(source, "/"),
            DiagnosticCodes.INPUT_PARSE_ERROR,
            "geometry root must be an object",
            "Use a Bedrock geometry document object");
      }
      if (jsonDepth(root, 0) > safe.limits().maxNestingDepth()) {
        return limitFailure(
            location(source, "/"),
            DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
            "JSON nesting depth exceeds configured limit",
            "Increase maxNestingDepth",
            "maxNestingDepth",
            safe.limits().maxNestingDepth(),
            jsonDepth(root, 0));
      }
      return parseRoot(root, source, safe);
    } catch (IOException exception) {
      return failure(
          location(source, ""),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "malformed geometry JSON",
          "Fix the JSON syntax");
    }
  }

  private Result<ParsedGeometry> parseRoot(
      JsonNode root, SourcePath source, GeometryParseRequest request) {
    JsonNode version = root.get("format_version");
    if (version == null || !version.isTextual() || !"1.12.0".equals(version.textValue())) {
      return failure(
          location(source, "/format_version"),
          DiagnosticCodes.INPUT_UNSUPPORTED_VERSION,
          "only format_version 1.12.0 is supported",
          "Set format_version to 1.12.0");
    }
    JsonNode geometries = root.get("minecraft:geometry");
    if (geometries == null || !geometries.isArray()) {
      return failure(
          location(source, "/minecraft:geometry"),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "minecraft:geometry must be an array",
          "Add a geometry array");
    }
    if (geometries.size() == 0) {
      return failure(
          location(source, "/minecraft:geometry"),
          DiagnosticCodes.GEO_MODEL_NOT_FOUND,
          "geometry array is empty",
          "Provide one geometry description");
    }
    if (geometries.size() > request.limits().maxGeometries()) {
      return limitFailure(
          location(source, "/minecraft:geometry"),
          DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
          "geometry count exceeds configured limit",
          "Increase maxGeometries",
          "maxGeometries",
          request.limits().maxGeometries(),
          geometries.size());
    }
    int selected = selectGeometry(geometries, request.geometryId(), source);
    if (selected < 0) {
      String code =
          selected == -2
              ? DiagnosticCodes.GEO_MODEL_AMBIGUOUS
              : request.geometryId() == null
                  ? DiagnosticCodes.GEO_MULTIPLE_MODELS
                  : DiagnosticCodes.GEO_MODEL_NOT_FOUND;
      return failure(
          location(source, "/minecraft:geometry"),
          code,
          selected == -2
              ? "requested geometry identifier is ambiguous"
              : request.geometryId() == null
                  ? "multiple geometries require an explicit geometryId"
                  : "requested geometry identifier was not found",
          selected == -2
              ? "Use an identifier that occurs exactly once"
              : request.geometryId() == null ? "Provide geometryId" : "Use an exact identifier");
    }
    JsonNode geometry = geometries.get(selected);
    JsonNode description = geometry.get("description");
    if (description == null || !description.isObject()) {
      return failure(
          location(source, pointer(selected, "description")),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "selected geometry description is missing",
          "Add a description object");
    }
    String identifier = text(description, "identifier");
    if (identifier == null || identifier.isBlank()) {
      return failure(
          location(source, pointer(selected, "description/identifier")),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "geometry identifier is missing",
          "Add a non-empty identifier");
    }
    if (identifier.length() > request.limits().maxStringLength()) {
      return limitFailure(
          location(source, pointer(selected, "description/identifier")),
          DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
          "geometry identifier exceeds maxStringLength",
          "Increase maxStringLength",
          "maxStringLength",
          request.limits().maxStringLength(),
          identifier.length());
    }
    JsonNode textureWidthNode = description.get("texture_width");
    if (textureWidthNode != null && !textureWidthNode.isIntegralNumber()) {
      return failure(
          location(source, pointer(selected, "description/texture_width")),
          DiagnosticCodes.IR_INVALID_VALUE,
          "texture_width must be an integer",
          "Use a finite integer texture width");
    }
    JsonNode textureHeightNode = description.get("texture_height");
    if (textureHeightNode != null && !textureHeightNode.isIntegralNumber()) {
      return failure(
          location(source, pointer(selected, "description/texture_height")),
          DiagnosticCodes.IR_INVALID_VALUE,
          "texture_height must be an integer",
          "Use a finite integer texture height");
    }
    GeometryId geometryId = new GeometryId(identifier);
    int textureWidth = integer(description, "texture_width", 0);
    int textureHeight = integer(description, "texture_height", 0);
    JsonNode bonesNode = geometry.get("bones");
    if (bonesNode == null) bonesNode = JSON.createArrayNode();
    if (!bonesNode.isArray()) {
      return failure(
          location(source, pointer(selected, "bones")),
          DiagnosticCodes.INPUT_PARSE_ERROR,
          "bones must be an array",
          "Use an array of bone objects");
    }
    if (bonesNode.size() > request.limits().maxBones()) {
      return limitFailure(
          location(source, pointer(selected, "bones")),
          DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
          "bone count exceeds configured limit",
          "Increase maxBones",
          "maxBones",
          request.limits().maxBones(),
          bonesNode.size());
    }
    DiagnosticBag diagnostics = new DiagnosticBag();
    List<BoneData> data = new ArrayList<>();
    Map<String, BoneData> byName = new LinkedHashMap<>();
    int cubeCount = 0;
    for (int i = 0; i < bonesNode.size(); i++) {
      JsonNode node = bonesNode.get(i);
      String base = pointer(selected, "bones/" + i);
      if (!node.isObject()) {
        diagnostics =
            diagnostics.add(
                error(
                    source,
                    base,
                    DiagnosticCodes.INPUT_PARSE_ERROR,
                    "bone must be an object",
                    "Use a bone object",
                    Map.of()));
        continue;
      }
      String name = text(node, "name");
      if (name != null && name.length() > request.limits().maxStringLength()) {
        diagnostics =
            diagnostics.add(
                limitDiagnostic(
                    source,
                    base + "/name",
                    "maxStringLength",
                    request.limits().maxStringLength(),
                    name.length(),
                    "Increase maxStringLength"));
        continue;
      }
      if (name == null || name.isBlank()) {
        diagnostics =
            diagnostics.add(
                error(
                    source,
                    base + "/name",
                    DiagnosticCodes.IR_INVALID_ID,
                    "bone name is invalid",
                    "Use a non-empty bounded name",
                    Map.of()));
        continue;
      }
      if (byName.containsKey(name)) {
        diagnostics =
            diagnostics.add(
                error(
                    source,
                    base + "/name",
                    DiagnosticCodes.GEO_DUPLICATE_BONE_NAME,
                    "duplicate bone name",
                    "Rename the bone",
                    Map.of("name", name)));
        continue;
      }
      BoneId id = new BoneId(identifier + "/bone/" + i);
      SourceLocation boneSource = location(source, base);
      Vec3d pivot = vector(node, "pivot", Vec3d.ZERO, source, base + "/pivot", diagnostics);
      diagnostics = LAST_DIAGNOSTICS.get();
      Vec3d rotation =
          vector(node, "rotation", Vec3d.ZERO, source, base + "/rotation", diagnostics);
      diagnostics = LAST_DIAGNOSTICS.get();
      Quatd quaternion =
          Quatd.fromEulerZYX(
              Math.toRadians(-rotation.x()),
              Math.toRadians(-rotation.y()),
              Math.toRadians(rotation.z()));
      double inflate = number(node, "inflate", 0.0, source, base + "/inflate", diagnostics);
      diagnostics = LAST_DIAGNOSTICS.get();
      JsonNode mirrorNode = node.get("mirror");
      if (mirrorNode != null && !mirrorNode.isBoolean()) {
        diagnostics =
            diagnostics.add(
                error(
                    source,
                    base + "/mirror",
                    DiagnosticCodes.IR_INVALID_VALUE,
                    "bone mirror must be boolean",
                    "Use true or false",
                    Map.of("field", "mirror")));
      }
      boolean mirror = mirrorNode != null && mirrorNode.isBoolean() && mirrorNode.booleanValue();
      BoneData bone =
          new BoneData(
              id, name, text(node, "parent"), pivot, quaternion, inflate, mirror, i, boneSource);
      JsonNode cubes = node.get("cubes");
      if (cubes != null && !cubes.isArray()) {
        diagnostics =
            diagnostics.add(
                error(
                    source,
                    base + "/cubes",
                    DiagnosticCodes.INPUT_PARSE_ERROR,
                    "cubes must be an array",
                    "Use an array of cubes",
                    Map.of()));
      } else if (cubes != null) {
        if (cubes.size() > request.limits().maxCubesPerBone()) {
          diagnostics =
              diagnostics.add(
                  error(
                      source,
                      base + "/cubes",
                      DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
                      "cube count exceeds configured limit",
                      "Increase maxCubesPerBone",
                      Map.of(
                          "limitName",
                          "maxCubesPerBone",
                          "limit",
                          Integer.toString(request.limits().maxCubesPerBone()),
                          "observed",
                          Integer.toString(cubes.size()))));
        }
        for (int c = 0; c < cubes.size(); c++) {
          ParsedCube parsed =
              parseCube(cubes.get(c), source, base + "/cubes/" + c, id, c, node, diagnostics);
          diagnostics = LAST_DIAGNOSTICS.get();
          if (parsed != null) {
            bone.cubes.add(parsed);
            cubeCount++;
          }
        }
      }
      data.add(bone);
      byName.put(name, bone);
    }
    if (cubeCount > request.limits().maxTotalCubes()) {
      diagnostics =
          diagnostics.add(
              error(
                  source,
                  pointer(selected, "bones"),
                  DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
                  "total cube count exceeds configured limit",
                  "Increase maxTotalCubes",
                  Map.of(
                      "limitName",
                      "maxTotalCubes",
                      "limit",
                      Integer.toString(request.limits().maxTotalCubes()),
                      "observed",
                      Integer.toString(cubeCount))));
    }
    List<BoneId> roots = new ArrayList<>();
    for (BoneData bone : data) {
      if (bone.parentName == null || bone.parentName.isBlank()) roots.add(bone.id);
      else {
        BoneData parent = byName.get(bone.parentName);
        if (parent == null)
          diagnostics =
              diagnostics.add(
                  error(
                      source,
                      bone.source.jsonPointer(),
                      DiagnosticCodes.GEO_PARENT_NOT_FOUND,
                      "parent bone was not found",
                      "Declare the parent before use",
                      Map.of("parent", bone.parentName)));
        else parent.children.add(bone.id);
      }
    }
    if (hasCycle(data, byName))
      diagnostics =
          diagnostics.add(
              error(
                  source,
                  pointer(selected, "bones"),
                  DiagnosticCodes.GEO_HIERARCHY_CYCLE,
                  "bone hierarchy contains a cycle",
                  "Break the parent cycle",
                  Map.of()));
    int hierarchyDepth =
        data.stream().mapToInt(bone -> depth(bone, byName, new HashSet<>())).max().orElse(0);
    if (hierarchyDepth > request.limits().maxHierarchyDepth())
      diagnostics =
          diagnostics.add(
              error(
                  source,
                  pointer(selected, "bones"),
                  DiagnosticCodes.INPUT_LIMIT_EXCEEDED,
                  "hierarchy depth exceeds configured limit",
                  "Increase maxHierarchyDepth",
                  Map.of(
                      "limit", Integer.toString(request.limits().maxHierarchyDepth()),
                      "observed", Integer.toString(hierarchyDepth),
                      "limitName", "maxHierarchyDepth")));
    List<ParsedBone> parsedBones = data.stream().map(bone -> bone.toParsed(byName)).toList();
    List<FeatureOccurrence> unsupported = new ArrayList<>();
    for (BoneData bone : data) {
      JsonNode sourceNode = bonesNode.get(bone.sourceIndex);
      for (String field :
          List.of(
              "poly_mesh",
              "texture_meshes",
              "locators",
              "render_group_id",
              "debug",
              "never_render",
              "reset")) {
        if (sourceNode != null && sourceNode.has(field)) {
          SourceLocation featureSource =
              location(source, pointer(selected, "bones/" + bone.sourceIndex + "/" + field));
          unsupported.add(new FeatureOccurrence(field, featureSource));
          String code =
              field.equals("poly_mesh")
                  ? DiagnosticCodes.GEO_MESH_UNSUPPORTED
                  : DiagnosticCodes.GEO_FEATURE_UNSUPPORTED;
          diagnostics =
              diagnostics.add(
                  new Diagnostic(
                      field.equals("poly_mesh") ? Severity.ERROR : Severity.WARNING,
                      DiagnosticCode.fromCatalog(code),
                      featureSource,
                      "geometry feature is outside T200",
                      "Defer this feature to a later phase",
                      bone.name,
                      null,
                      new java.util.TreeMap<>(Map.of("feature", field))));
        }
      }
    }
    ParsedGeometry result =
        new ParsedGeometry(
            source, geometryId, textureWidth, textureHeight, parsedBones, roots, unsupported);
    if (diagnostics.hasErrors()) return Result.failure(diagnostics);
    Result<ParsedGeometry> validated = new ParsedGeometryValidator().validate(result);
    DiagnosticBag combined = diagnostics.addAll(validated.diagnostics());
    return combined.hasErrors() ? Result.failure(combined) : Result.success(result, combined);
  }

  private static final ThreadLocal<DiagnosticBag> LAST_DIAGNOSTICS =
      ThreadLocal.withInitial(DiagnosticBag::new);

  private static Vec3d vector(
      JsonNode node,
      String field,
      Vec3d fallback,
      SourcePath source,
      String pointer,
      DiagnosticBag diagnostics) {
    LAST_DIAGNOSTICS.set(diagnostics);
    JsonNode value = node.get(field);
    if (value == null) return fallback;
    if (!value.isArray() || value.size() != 3) {
      LAST_DIAGNOSTICS.set(
          diagnostics.add(
              error(
                  source,
                  pointer,
                  DiagnosticCodes.IR_INVALID_VALUE,
                  "vector must contain exactly three finite numbers",
                  "Provide [x, y, z]",
                  Map.of())));
      return fallback;
    }
    double[] values = new double[3];
    for (int i = 0; i < 3; i++) {
      if (!value.get(i).isNumber() || !Double.isFinite(value.get(i).asDouble())) {
        LAST_DIAGNOSTICS.set(
            diagnostics.add(
                error(
                    source,
                    pointer + "/" + i,
                    DiagnosticCodes.IR_INVALID_VALUE,
                    "vector component must be finite",
                    "Use a finite number",
                    Map.of())));
        return fallback;
      }
      values[i] = value.get(i).asDouble();
    }
    return new Vec3d(values[0], values[1], values[2]);
  }

  private static ParsedCube parseCube(
      JsonNode node,
      SourcePath source,
      String pointer,
      BoneId boneId,
      int index,
      JsonNode boneNode,
      DiagnosticBag diagnostics) {
    LAST_DIAGNOSTICS.set(diagnostics);
    if (!node.isObject()) {
      LAST_DIAGNOSTICS.set(
          diagnostics.add(
              error(
                  source,
                  pointer,
                  DiagnosticCodes.INPUT_PARSE_ERROR,
                  "cube must be an object",
                  "Use a cube object",
                  Map.of())));
      return null;
    }
    Vec3d origin = vector(node, "origin", Vec3d.ZERO, source, pointer + "/origin", diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    Vec3d size = vector(node, "size", Vec3d.ZERO, source, pointer + "/size", diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    if (size.x() < 0 || size.y() < 0 || size.z() < 0) {
      diagnostics =
          diagnostics.add(
              error(
                  source,
                  pointer + "/size",
                  DiagnosticCodes.IR_INVALID_VALUE,
                  "cube size cannot be negative",
                  "Use non-negative dimensions",
                  Map.of()));
    }
    Vec3d pivot = vector(node, "pivot", Vec3d.ZERO, source, pointer + "/pivot", diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    Vec3d rotation =
        vector(node, "rotation", Vec3d.ZERO, source, pointer + "/rotation", diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    double boneInflate = number(boneNode, "inflate", 0, source, pointer, diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    double inflate =
        number(node, "inflate", boneInflate, source, pointer + "/inflate", diagnostics);
    diagnostics = LAST_DIAGNOSTICS.get();
    JsonNode mirrorNode = node.get("mirror");
    if (mirrorNode != null && !mirrorNode.isBoolean()) {
      diagnostics =
          diagnostics.add(
              error(
                  source,
                  pointer + "/mirror",
                  DiagnosticCodes.IR_INVALID_VALUE,
                  "cube mirror must be boolean",
                  "Use true or false",
                  Map.of("field", "mirror")));
    }
    boolean mirror = mirrorNode != null && mirrorNode.isBoolean() && mirrorNode.booleanValue();
    LAST_DIAGNOSTICS.set(diagnostics);
    RawUvBoundary rawUv = node.has("uv") ? new RawUvBoundary(node.get("uv").toString()) : null;
    return new ParsedCube(
        new CubeId(boneId.value() + "/cube/" + index),
        boneId,
        origin,
        size,
        pivot,
        rotation,
        inflate,
        mirror,
        rawUv,
        location(source, pointer));
  }

  private static double number(
      JsonNode node,
      String field,
      double fallback,
      SourcePath source,
      String pointer,
      DiagnosticBag diagnostics) {
    LAST_DIAGNOSTICS.set(diagnostics);
    JsonNode value = node.get(field);
    if (value == null) return fallback;
    if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
      LAST_DIAGNOSTICS.set(
          diagnostics.add(
              error(
                  source,
                  pointer.endsWith("/" + field) ? pointer : pointer + "/" + field,
                  DiagnosticCodes.IR_INVALID_VALUE,
                  "value must be finite",
                  "Use a finite number",
                  Map.of())));
      return fallback;
    }
    return value.asDouble();
  }

  private static boolean hasCycle(List<BoneData> bones, Map<String, BoneData> byName) {
    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();
    for (BoneData bone : bones) if (cycle(bone, byName, visiting, visited)) return true;
    return false;
  }

  private static boolean cycle(
      BoneData bone, Map<String, BoneData> byName, Set<String> visiting, Set<String> visited) {
    if (visited.contains(bone.name)) return false;
    if (!visiting.add(bone.name)) return true;
    if (bone.parentName != null
        && byName.containsKey(bone.parentName)
        && cycle(byName.get(bone.parentName), byName, visiting, visited)) return true;
    visiting.remove(bone.name);
    visited.add(bone.name);
    return false;
  }

  private static int selectGeometry(JsonNode geometries, GeometryId requested, SourcePath source) {
    if (requested == null) return geometries.size() == 1 ? 0 : -1;
    int found = -1;
    for (int i = 0; i < geometries.size(); i++) {
      String id = text(geometries.get(i).path("description"), "identifier");
      if (requested.value().equals(id)) {
        if (found >= 0) return -2;
        found = i;
      }
    }
    return found;
  }

  private static int depth(BoneData bone, Map<String, BoneData> byName, Set<String> seen) {
    if (!seen.add(bone.name)) return Integer.MAX_VALUE;
    if (bone.parentName == null || !byName.containsKey(bone.parentName)) return 1;
    return 1 + depth(byName.get(bone.parentName), byName, seen);
  }

  private static int jsonDepth(JsonNode node, int current) {
    if (!node.isContainerNode()) return current;
    int deepest = current + 1;
    for (JsonNode child : node) deepest = Math.max(deepest, jsonDepth(child, current + 1));
    return deepest;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && value.isTextual() ? value.textValue() : null;
  }

  private static int integer(JsonNode node, String field, int fallback) {
    JsonNode value = node.get(field);
    return value != null && value.isIntegralNumber() ? value.intValue() : fallback;
  }

  private static String pointer(int geometry, String suffix) {
    return "/minecraft:geometry/" + geometry + "/" + suffix;
  }

  private static SourceLocation location(SourcePath source, String pointer) {
    return new SourceLocation(
        source, null, null, pointer == null || pointer.isEmpty() ? null : pointer, null);
  }

  private static SourceLocation source(Path path, String pointer) {
    String logical =
        path.getFileName() == null ? "geometry.geo.json" : path.getFileName().toString();
    return location(new SourcePath(logical), pointer);
  }

  private static SourcePath sourcePath(Path path) {
    Path normalized = path.normalize();
    String logical;
    if (normalized.isAbsolute()) {
      try {
        logical = Path.of("").toAbsolutePath().normalize().relativize(normalized).toString();
      } catch (IllegalArgumentException exception) {
        logical =
            normalized.getFileName() == null
                ? "geometry.geo.json"
                : normalized.getFileName().toString();
      }
    } else {
      logical = normalized.toString();
    }
    return new SourcePath(logical);
  }

  private static Diagnostic error(
      SourcePath source,
      String pointer,
      String code,
      String message,
      String suggestion,
      Map<String, String> context) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        location(source, pointer),
        message,
        suggestion,
        null,
        null,
        new java.util.TreeMap<>(context));
  }

  private static <T> Result<T> failure(
      SourceLocation location, String code, String message, String suggestion) {
    return Result.failure(
        new DiagnosticBag()
            .add(
                new Diagnostic(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(code),
                    location,
                    message,
                    suggestion,
                    null,
                    null,
                    new java.util.TreeMap<>())));
  }

  private static <T> Result<T> limitFailure(
      SourceLocation location,
      String code,
      String message,
      String suggestion,
      String limitName,
      long limit,
      long observed) {
    return Result.failure(
        new DiagnosticBag()
            .add(
                new Diagnostic(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(code),
                    location,
                    message,
                    suggestion,
                    null,
                    null,
                    new java.util.TreeMap<>(
                        Map.of(
                            "limitName", limitName,
                            "limit", Long.toString(limit),
                            "observed", Long.toString(observed))))));
  }

  private static Diagnostic limitDiagnostic(
      SourcePath source,
      String pointer,
      String name,
      long limit,
      long observed,
      String suggestion) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_LIMIT_EXCEEDED),
        location(source, pointer),
        "geometry parser limit exceeded: " + name,
        suggestion,
        null,
        null,
        new java.util.TreeMap<>(
            Map.of(
                "limitName", name,
                "limit", Long.toString(limit),
                "observed", Long.toString(observed))));
  }

  private static final class BoneData {
    private final BoneId id;
    private final String name;
    private final String parentName;
    private final Vec3d pivot;
    private final Quatd rotation;
    private final double inflate;
    private final boolean mirror;
    private final int sourceIndex;
    private final SourceLocation source;
    private final List<BoneId> children = new ArrayList<>();
    private final List<ParsedCube> cubes = new ArrayList<>();

    private BoneData(
        BoneId id,
        String name,
        String parentName,
        Vec3d pivot,
        Quatd rotation,
        double inflate,
        boolean mirror,
        int sourceIndex,
        SourceLocation source) {
      this.id = id;
      this.name = name;
      this.parentName = parentName;
      this.pivot = pivot;
      this.rotation = rotation;
      this.inflate = inflate;
      this.mirror = mirror;
      this.sourceIndex = sourceIndex;
      this.source = source;
    }

    private ParsedBone toParsed(Map<String, BoneData> byName) {
      Vec3d translation = pivot;
      BoneId parent = null;
      if (parentName != null && byName.containsKey(parentName)) {
        BoneData parentBone = byName.get(parentName);
        parent = parentBone.id;
        Vec3d delta = pivot.subtract(parentBone.pivot);
        translation = new Vec3d(-delta.x(), -delta.y(), delta.z());
      } else {
        translation = new Vec3d(-pivot.x(), -pivot.y(), pivot.z());
      }
      return new ParsedBone(
          id,
          name,
          parent,
          children,
          new Transform(translation, rotation, new Vec3d(1, 1, 1)),
          inflate,
          mirror,
          cubes,
          source);
    }
  }
}
