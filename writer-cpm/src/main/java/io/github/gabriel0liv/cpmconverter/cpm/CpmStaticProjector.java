package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIrValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Projects validated ModelIR geometry into a deterministic CPM V1-facing static graph. */
public final class CpmStaticProjector {
  private static final double CPM_MIN_SCALE = 0.01;

  public Result<CpmStaticProjectV1> project(ModelIR model, CpmProjectionSettings settings) {
    if (model == null) return failure(DiagnosticCodes.INTERNAL_ERROR, "model is null");
    if (settings == null
        || !Double.isFinite(settings.modelScale())
        || !Double.isFinite(settings.verticalOffset())) {
      return failure(
          DiagnosticCodes.CPM_PROJECTION_INVALID_SETTING, "CPM projection settings must be finite");
    }
    if (settings.modelScale() < CPM_MIN_SCALE) {
      return failure(
          DiagnosticCodes.CPM_PROJECTION_MODEL_SCALE,
          "modelScale must be at least 0.01 for exact CPM 0.6.27 representation");
    }

    var validation = new ModelIrValidator().validate(model);
    if (validation.hasErrors()) return Result.failure(validation);

    try {
      CpmElementTreeProjector treeProjector = new CpmElementTreeProjector(model);
      CpmElementV1 entityRoot =
          treeProjector.entityRoot(settings.modelScale(), settings.verticalOffset());
      List<CpmRootV1> roots = vanillaRoots(entityRoot, settings);
      LinkedHashMap<ProjectionKey, CpmElementV1> logicalTargets = new LinkedHashMap<>();
      indexPreorder(entityRoot, logicalTargets);
      return Result.success(new CpmStaticProjectV1(roots, model.textures(), logicalTargets));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      String message =
          exception.getMessage() == null
              ? "invalid static projection value"
              : exception.getMessage();
      return failure(DiagnosticCodes.IR_INVALID_VALUE, message);
    }
  }

  private List<CpmRootV1> vanillaRoots(CpmElementV1 entityRoot, CpmProjectionSettings settings) {
    List<CpmRootV1> roots = new ArrayList<>();
    for (CpmVanillaPart part : CpmVanillaPart.values()) {
      roots.add(
          new CpmRootV1(
              part,
              !settings.hideVanillaRoots(),
              settings.disableVanillaAnim(),
              part == CpmVanillaPart.BODY ? List.of(entityRoot) : List.of()));
    }
    return List.copyOf(roots);
  }

  private void indexPreorder(
      CpmElementV1 element, LinkedHashMap<ProjectionKey, CpmElementV1> targets) {
    if (targets.putIfAbsent(element.key(), element) != null) {
      throw new IllegalStateException("duplicate projection key " + element.key().value());
    }
    for (CpmElementV1 child : element.children()) indexPreorder(child, targets);
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }
}
