package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

public final class CpmLogicalProjectionValidator {
  public DiagnosticBag validate(
      CpmStaticProjection projection,
      Set<io.github.gabriel0liv.cpmconverter.ir.BoneId> expectedBones,
      Set<io.github.gabriel0liv.cpmconverter.ir.CubeId> expectedCubes) {
    DiagnosticBag bag = validate(projection);
    if (projection != null) {
      if (!projection.index().boneTargets().keySet().equals(expectedBones))
        bag =
            bag.add(
                Diagnostic.of(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                    "bone target index does not match source"));
      if (!projection.index().cubeTargets().keySet().equals(expectedCubes))
        bag =
            bag.add(
                Diagnostic.of(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                    "cube target index does not match source"));
    }
    return bag;
  }

  public DiagnosticBag validate(CpmStaticProjection projection) {
    DiagnosticBag bag = new DiagnosticBag();
    if (projection == null || projection.project() == null)
      return bag.add(
          Diagnostic.of(
              Severity.ERROR,
              DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
              "projection is null"));
    var p = projection.project();
    if (p.version() != 1 || p.roots().size() != 6)
      bag =
          bag.add(
              Diagnostic.of(
                  Severity.ERROR,
                  DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                  "CPM V1 requires six roots"));
    var keys = new HashSet<String>();
    for (var r : p.roots()) {
      if (!keys.add("root:" + r.root().id()))
        bag =
            bag.add(
                Diagnostic.of(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                    "duplicate root"));
      bag = bag.addAll(check(r.children(), keys));
    }
    if (projection.index().boneTargets().isEmpty() && projection.index().cubeTargets().isEmpty())
      bag =
          bag.add(
              Diagnostic.of(
                  Severity.ERROR,
                  DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                  "projection index is empty"));
    return bag;
  }

  private DiagnosticBag check(List<CpmLogicalElementV1> nodes, Set<String> keys) {
    DiagnosticBag bag = new DiagnosticBag();
    for (var n : nodes) {
      if (!keys.add(n.key().value()))
        bag =
            bag.add(
                Diagnostic.of(
                    Severity.ERROR,
                    DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_VALIDATION_FAILED),
                    "duplicate node key"));
      bag = bag.addAll(check(n.children(), keys));
    }
    return bag;
  }
}
