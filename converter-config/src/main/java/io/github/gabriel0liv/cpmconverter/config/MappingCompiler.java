package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticBag;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import io.github.gabriel0liv.cpmconverter.ir.ModelIndex;
import java.util.LinkedHashMap;

/** Resolves user names to stable IR IDs without choosing a CPM topology. */
public final class MappingCompiler {
    public Result<SemanticRigMap> compile(MappingDocumentV1 document, ModelIndex index) {
        DiagnosticBag diagnostics = new DiagnosticBag();
        var bones = new LinkedHashMap<String, BoneId>();
        for (var entry : document.bones().entrySet()) {
            var result = index.bone(entry.getValue());
            diagnostics = diagnostics.addAll(result.diagnostics());
            if (result.success()) bones.put(entry.getKey(), result.value());
        }
        var clips = new LinkedHashMap<String, ClipId>();
        for (var entry : document.clips().entrySet()) {
            var result = index.clip(entry.getValue());
            diagnostics = diagnostics.addAll(result.diagnostics());
            if (result.success()) clips.put(entry.getKey(), result.value());
        }
        if (diagnostics.hasErrors()) return Result.failure(diagnostics);
        SemanticRigMap compiled = new SemanticRigMap(bones, clips, document.ignore(), document.modelScale(),
                document.verticalOffset(), document.skin(), document.rootStrategy(), document.rootRoles(),
                document.look(), document.stateMappings(), document.sampling(), document.diagnosticPolicy());
        return Result.success(compiled, diagnostics);
    }
}
