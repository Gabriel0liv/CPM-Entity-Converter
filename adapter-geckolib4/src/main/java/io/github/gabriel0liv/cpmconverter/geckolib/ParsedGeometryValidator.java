package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/** Validates graph and ownership invariants of the T200 parsed boundary. */
public final class ParsedGeometryValidator {
  public Result<ParsedGeometry> validate(ParsedGeometry geometry) {
    if (geometry == null) {
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.INPUT_PARSE_ERROR),
              null,
              "parsed geometry is null",
              "Provide a parsed geometry",
              null,
              null,
              new TreeMap<>()));
    }
    DiagnosticBag diagnostics = new DiagnosticBag();
    Set<String> ids = new HashSet<>();
    Set<String> names = new HashSet<>();
    Set<String> roots = new HashSet<>();
    for (ParsedBone bone : geometry.bones()) {
      if (!ids.add(bone.id().value())) {
        diagnostics = diagnostics.add(error(DiagnosticCodes.IR_DUPLICATE_BONE_ID, bone.source()));
      }
      if (!names.add(bone.sourceName())) {
        diagnostics =
            diagnostics.add(error(DiagnosticCodes.GEO_DUPLICATE_BONE_NAME, bone.source()));
      }
      if (bone.parent() == null) roots.add(bone.id().value());
      else if (geometry.bones().stream()
          .noneMatch(candidate -> candidate.id().equals(bone.parent()))) {
        diagnostics = diagnostics.add(error(DiagnosticCodes.GEO_PARENT_NOT_FOUND, bone.source()));
      }
      Set<String> children = new HashSet<>();
      for (var child : bone.children()) {
        if (!children.add(child.value())) {
          diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CHILD_DUPLICATE, bone.source()));
        }
        if (geometry.bones().stream().noneMatch(candidate -> candidate.id().equals(child))) {
          diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CHILD_MISSING, bone.source()));
        }
      }
      for (ParsedCube cube : bone.cubes()) {
        if (!cube.boneId().equals(bone.id())) {
          diagnostics = diagnostics.add(error(DiagnosticCodes.IR_CUBE_BONE_MISSING, cube.source()));
        }
      }
    }
    if (!roots.equals(
        geometry.roots().stream()
            .map(id -> id.value())
            .collect(java.util.stream.Collectors.toSet()))) {
      diagnostics =
          diagnostics.add(
              error(
                  DiagnosticCodes.IR_PARENT_CHILD_MISMATCH,
                  io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation.of(
                      geometry.source())));
    }
    return diagnostics.hasErrors()
        ? Result.failure(diagnostics)
        : Result.success(geometry, diagnostics);
  }

  private static Diagnostic error(
      String code, io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation location) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(code),
        location,
        "parsed geometry invariant failed",
        "Correct the geometry graph",
        null,
        null,
        new TreeMap<>());
  }
}
