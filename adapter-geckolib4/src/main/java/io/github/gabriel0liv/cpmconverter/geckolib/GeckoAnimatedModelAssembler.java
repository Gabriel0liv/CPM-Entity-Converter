package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIrValidator;
import java.util.List;

public final class GeckoAnimatedModelAssembler {
  public Result<ModelIR> attach(ModelIR model, List<AnimationClipIR> clips) {
    if (model == null) throw new IllegalArgumentException("model");
    ModelIR result =
        new ModelIR(
            model.source(),
            model.geometryId(),
            model.bones(),
            model.roots(),
            clips,
            model.textures(),
            model.unsupportedFeatures());
    DiagnosticBag diagnostics = new ModelIrValidator().validate(result);
    return diagnostics.hasErrors()
        ? Result.failure(diagnostics)
        : Result.success(result, diagnostics);
  }
}
