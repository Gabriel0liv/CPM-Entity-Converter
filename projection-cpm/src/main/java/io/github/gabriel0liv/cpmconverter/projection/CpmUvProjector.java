package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.util.*;

public final class CpmUvProjector {
  public Result<CpmUvV1> project(CubeIR cube) {
    if (cube == null)
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.UV_INVALID),
              "cube is null"));
    if (cube.uv() instanceof BoxUvIR box) {
      if (!integer(box.u())
          || !integer(box.v())
          || !integer(cube.size().x())
          || !integer(cube.size().y())
          || !integer(cube.size().z()))
        return Result.failure(error(cube, "box UV and cube size must be integral"));
      return Result.success(new CpmBoxUvV1((int) box.u(), (int) box.v()));
    }
    PerFaceUvIR per = (PerFaceUvIR) cube.uv();
    EnumMap<CpmCubeFace, CpmFaceUvV1> out = new EnumMap<>(CpmCubeFace.class);
    for (var e : per.faces().entrySet()) {
      FaceUvIR f = e.getValue();
      double ex = f.u() + f.width(), ey = f.v() + f.height();
      if (!integer(f.u()) || !integer(f.v()) || !integer(ex) || !integer(ey))
        return Result.failure(error(cube, "per-face UV coordinates must be integral"));
      CpmCubeFace face = CpmCubeFace.valueOf(e.getKey().name());
      out.put(
          face,
          new CpmFaceUvV1(
              (int) f.u(),
              (int) f.v(),
              (int) ex,
              (int) ey,
              (face == CpmCubeFace.UP || face == CpmCubeFace.DOWN)
                  ? CpmUvRotation.ROT_180
                  : CpmUvRotation.ROT_0,
              false));
    }
    return Result.success(new CpmPerFaceUvV1(out));
  }

  private static boolean integer(double v) {
    return Double.isFinite(v)
        && v == Math.rint(v)
        && v >= Integer.MIN_VALUE
        && v <= Integer.MAX_VALUE;
  }

  private static Diagnostic error(CubeIR c, String msg) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(DiagnosticCodes.UV_INVALID),
        c.provenance(),
        msg,
        "Use integer box UV values or authored per-face UV",
        c.bone().value(),
        null,
        new TreeMap<>(Map.of("cubeId", c.id().value(), "reason", "CPM V1 integer UV baseline")));
  }
}
