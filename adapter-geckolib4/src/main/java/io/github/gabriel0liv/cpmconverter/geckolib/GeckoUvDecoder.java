package io.github.gabriel0liv.cpmconverter.geckolib;

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
      return Result.failure(error(DiagnosticCodes.UV_INVALID, "UV is required", cube));
    try {
      JsonNode node = JSON.readTree(raw.canonicalJson());
      if (node == null)
        return Result.failure(error(DiagnosticCodes.UV_INVALID, "UV is empty", cube));
      if (node.isArray()) {
        if (node.size() != 2 || !node.get(0).isNumber() || !node.get(1).isNumber())
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "box UV requires two numbers", cube));
        double u = node.get(0).doubleValue(), v = node.get(1).doubleValue();
        if (!Double.isFinite(u) || !Double.isFinite(v))
          return Result.failure(error(DiagnosticCodes.UV_INVALID, "UV must be finite", cube));
        return Result.success(
            new BoxUvIR(u, v),
            bounds(
                cube,
                "box",
                u,
                v,
                Math.floor(cube.size().x()) + Math.floor(cube.size().z()),
                Math.floor(cube.size().y()) + Math.floor(cube.size().z()),
                textureWidth,
                textureHeight));
      }
      if (!node.isObject() || node.isEmpty())
        return Result.failure(
            error(DiagnosticCodes.UV_INVALID, "per-face UV must be a non-empty object", cube));
      var faces = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
      DiagnosticBag warnings = new DiagnosticBag();
      var fields = node.fieldNames();
      while (fields.hasNext()) {
        String name = fields.next();
        CubeFaceIR face = face(name);
        if (face == null) {
          warnings =
              warnings.add(
                  error(DiagnosticCodes.UV_FACE_UNKNOWN, "unknown UV face: " + name, cube));
          continue;
        }
        JsonNode entry = node.get(name);
        if (!entry.isObject() || !pair(entry.get("uv")) || !pair(entry.get("uv_size")))
          return Result.failure(
              error(DiagnosticCodes.UV_INVALID, "face UV requires uv and uv_size pairs", cube));
        double u = entry.get("uv").get(0).doubleValue(), v = entry.get("uv").get(1).doubleValue();
        double w = entry.get("uv_size").get(0).doubleValue(),
            h = entry.get("uv_size").get(1).doubleValue();
        if (!finite(u, v, w, h))
          return Result.failure(error(DiagnosticCodes.UV_INVALID, "face UV must be finite", cube));
        faces.put(face, new FaceUvIR(u, v, w, h));
        warnings = warnings.addAll(bounds(cube, name, u, v, w, h, textureWidth, textureHeight));
      }
      if (faces.isEmpty())
        return Result.failure(error(DiagnosticCodes.UV_INVALID, "no valid UV faces", cube));
      return Result.success(new PerFaceUvIR(faces), warnings);
    } catch (Exception ex) {
      return Result.failure(error(DiagnosticCodes.UV_INVALID, "invalid UV JSON", cube));
    }
  }

  private static boolean pair(JsonNode n) {
    return n != null && n.isArray() && n.size() == 2 && n.get(0).isNumber() && n.get(1).isNumber();
  }

  private static boolean finite(double... values) {
    for (double v : values) if (!Double.isFinite(v)) return false;
    return true;
  }

  private static CubeFaceIR face(String n) {
    try {
      return CubeFaceIR.valueOf(n.toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      return null;
    }
  }

  private static DiagnosticBag bounds(
      ParsedCube cube, String face, double u, double v, double w, double h, int tw, int th) {
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
                  cube.source(),
                  "UV outside texture grid",
                  "correct UV coordinates or texture dimensions",
                  null,
                  null,
                  c));
    }
    return new DiagnosticBag();
  }

  private static Diagnostic error(String code, String message, ParsedCube cube) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        cube == null ? null : cube.source(),
        message,
        "correct the UV value",
        null,
        null,
        new TreeMap<>());
  }
}
