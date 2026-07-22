package io.github.gabriel0liv.cpmconverter.geckolib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

/** Decodes the raw Gecko UV boundary while retaining signed, fractional coordinates. */
public final class GeckoUvDecoder {
  private static final ObjectMapper JSON = new ObjectMapper();

  public Result<UvIR> decode(
      RawUvBoundary raw, ParsedCube cube, int textureWidth, int textureHeight) {
    if (raw == null || cube == null)
      return Result.failure(error(DiagnosticCodes.UV_MISSING, "UV is required", cube, "/uv"));
    try {
      JsonNode node = JSON.readTree(raw.canonicalJson());
      if (node == null)
        return Result.failure(error(DiagnosticCodes.UV_INVALID, "UV is empty", cube, "/uv"));
      if (node.isArray()) {
        if (node.size() != 2)
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "box UV requires two numbers", cube, "/uv"));
        if (!node.get(0).isNumber())
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "box UV component must be numeric", cube, "/uv/0"));
        if (!node.get(1).isNumber())
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "box UV component must be numeric", cube, "/uv/1"));
        double u = node.get(0).doubleValue(), v = node.get(1).doubleValue();
        if (!Double.isFinite(u) || !Double.isFinite(v))
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "UV must be finite", cube, "/uv"));
        var warnings = new DiagnosticBag();
        for (var entry : GeckoBoxUvLayout.derive(new BoxUvIR(u, v), cube.size()).entrySet()) {
          var faceUv = entry.getValue();
          warnings =
              warnings.addAll(
                  bounds(
                      cube,
                      entry.getKey().name().toLowerCase(Locale.ROOT),
                      faceUv.u(),
                      faceUv.v(),
                      faceUv.width(),
                      faceUv.height(),
                      textureWidth,
                      textureHeight,
                      "/uv/" + entry.getKey().name().toLowerCase(Locale.ROOT)));
        }
        return Result.success(new BoxUvIR(u, v), warnings);
      }
      if (!node.isObject() || node.isEmpty())
        return Result.failure(
            error(
                DiagnosticCodes.UV_INVALID, "per-face UV must be a non-empty object", cube, "/uv"));
      var faces = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
      DiagnosticBag warnings = new DiagnosticBag();
      var fields = node.fieldNames();
      while (fields.hasNext()) {
        String name = fields.next();
        CubeFaceIR face = face(name);
        if (face == null) {
          return Result.failure(
              error(
                  DiagnosticCodes.UV_FACE_UNKNOWN,
                  "unknown UV face: " + name,
                  cube,
                  "/uv/" + name));
        }
        JsonNode entry = node.get(name);
        String facePointer = "/uv/" + name;
        if (!entry.isObject())
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face UV must be an object", cube, facePointer));
        if (!entry.has("uv"))
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face uv is required", cube, facePointer + "/uv"));
        if (!entry.has("uv_size"))
          return Result.failure(
              error(
                  DiagnosticCodes.UV_INVALID,
                  "face uv_size is required",
                  cube,
                  facePointer + "/uv_size"));
        if (!pair(entry.get("uv"))) {
          String p = invalidPairPointer(entry.get("uv"), facePointer + "/uv");
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face uv requires two numbers", cube, p));
        }
        if (!pair(entry.get("uv_size"))) {
          String p = invalidPairPointer(entry.get("uv_size"), facePointer + "/uv_size");
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face uv_size requires two numbers", cube, p));
        }
        double u = entry.get("uv").get(0).doubleValue(), v = entry.get("uv").get(1).doubleValue();
        double w = entry.get("uv_size").get(0).doubleValue(),
            h = entry.get("uv_size").get(1).doubleValue();
        if (!finite(u, v, w, h))
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face UV must be finite", cube, facePointer));
        faces.put(face, new FaceUvIR(u, v, w, h));
        warnings =
            warnings.addAll(
                bounds(cube, name, u, v, w, h, textureWidth, textureHeight, "/uv/" + name));
        if (entry.has("material_instance")) {
          var context = new TreeMap<String, String>();
          context.put("face", name);
          context.put("cubeId", cube.id().value());
          context.put("materialInstance", entry.get("material_instance").asText());
          warnings =
              warnings.add(
                  new Diagnostic(
                      Severity.WARNING,
                      DiagnosticCode.fromCatalog(DiagnosticCodes.UV_MATERIAL_INSTANCE_UNSUPPORTED),
                      location(cube, "/uv/" + name + "/material_instance"),
                      "material_instance is not routed",
                      "remove material_instance or defer routing",
                      cube.boneId().value(),
                      null,
                      context));
        }
      }
      if (faces.isEmpty())
        return Result.failure(error(DiagnosticCodes.UV_INVALID, "no valid UV faces", cube, "/uv"));
      return Result.success(new PerFaceUvIR(faces), warnings);
    } catch (JsonProcessingException ex) {
      return Result.failure(error(DiagnosticCodes.UV_INVALID, "invalid UV JSON", cube));
    }
  }

  private static boolean pair(JsonNode n) {
    return n != null && n.isArray() && n.size() == 2 && n.get(0).isNumber() && n.get(1).isNumber();
  }

  private static String invalidPairPointer(JsonNode n, String base) {
    if (n == null || !n.isArray() || n.size() != 2) return base;
    if (!n.get(0).isNumber()) return base + "/0";
    if (!n.get(1).isNumber()) return base + "/1";
    return base;
  }

  private static boolean finite(double... values) {
    for (double v : values) if (!Double.isFinite(v)) return false;
    return true;
  }

  private static CubeFaceIR face(String n) {
    try {
      return switch (n) {
        case "north" -> CubeFaceIR.NORTH;
        case "south" -> CubeFaceIR.SOUTH;
        case "east" -> CubeFaceIR.EAST;
        case "west" -> CubeFaceIR.WEST;
        case "up" -> CubeFaceIR.UP;
        case "down" -> CubeFaceIR.DOWN;
        default -> null;
      };
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static DiagnosticBag bounds(
      ParsedCube cube,
      String face,
      double u,
      double v,
      double w,
      double h,
      int tw,
      int th,
      String pointer) {
    double minU = Math.min(u, u + w),
        maxU = Math.max(u, u + w),
        minV = Math.min(v, v + h),
        maxV = Math.max(v, v + h);
    if (minU < 0 || minV < 0 || maxU > tw || maxV > th) {
      var c = new TreeMap<String, String>();
      c.put("face", face);
      c.put("u", Double.toString(u));
      c.put("v", Double.toString(v));
      c.put("width", Double.toString(w));
      c.put("height", Double.toString(h));
      c.put("textureWidth", Integer.toString(tw));
      c.put("textureHeight", Integer.toString(th));
      return new DiagnosticBag()
          .add(
              new Diagnostic(
                  Severity.WARNING,
                  DiagnosticCode.fromCatalog(DiagnosticCodes.UV_OUT_OF_BOUNDS),
                  location(cube, pointer),
                  "UV outside texture grid",
                  "correct UV coordinates or texture dimensions",
                  cube.boneId().value(),
                  null,
                  c));
    }
    return new DiagnosticBag();
  }

  private static Diagnostic error(String code, String message, ParsedCube cube) {
    return error(code, message, cube, "/uv");
  }

  private static Diagnostic error(String code, String message, ParsedCube cube, String pointer) {
    var context = new TreeMap<String, String>();
    if (cube != null) context.put("cubeId", cube.id().value());
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        cube == null ? null : location(cube, pointer),
        message,
        "correct the UV value",
        cube == null ? null : cube.boneId().value(),
        null,
        context);
  }

  private static SourceLocation location(ParsedCube cube, String pointer) {
    if (cube == null || cube.source() == null) return null;
    var s = cube.source();
    return new SourceLocation(s.source(), s.line(), s.column(), pointer, s.byteOffset());
  }
}
