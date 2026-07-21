package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.TreeMap;

/** Boundary factories convert predictable input failures into located diagnostics. */
public final class IrValueFactory {
  private IrValueFactory() {}

  public static Result<BoneId> boneId(String value, SourceLocation location) {
    return create(value, location, BoneId::new, DiagnosticCodes.IR_INVALID_ID, "bone id");
  }

  public static Result<CubeId> cubeId(String value, SourceLocation location) {
    return create(value, location, CubeId::new, DiagnosticCodes.IR_INVALID_ID, "cube id");
  }

  public static Result<GeometryId> geometryId(String value, SourceLocation location) {
    return create(value, location, GeometryId::new, DiagnosticCodes.IR_INVALID_ID, "geometry id");
  }

  private static <T> Result<T> create(
      String value,
      SourceLocation location,
      java.util.function.Function<String, T> constructor,
      String code,
      String label) {
    try {
      return Result.success(constructor.apply(value));
    } catch (IllegalArgumentException exception) {
      Diagnostic diagnostic =
          new Diagnostic(
              Severity.ERROR,
              new DiagnosticCode(code),
              location,
              "invalid " + label,
              "Provide a non-empty identifier",
              null,
              null,
              new TreeMap<>());
      return Result.failure(diagnostic);
    }
  }
}
