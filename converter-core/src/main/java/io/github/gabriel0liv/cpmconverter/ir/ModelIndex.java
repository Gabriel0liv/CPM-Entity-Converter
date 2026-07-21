package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ModelIndex {
  private final Map<String, List<BoneId>> bones;
  private final Map<String, ClipId> clips;

  public ModelIndex(ModelIR model) {
    var b = new LinkedHashMap<String, List<BoneId>>();
    for (var x : model.bones()) b.computeIfAbsent(x.name(), k -> new ArrayList<>()).add(x.id());
    var orderedBones = new LinkedHashMap<String, List<BoneId>>();
    b.forEach((name, ids) -> orderedBones.put(name, List.copyOf(ids)));
    bones = Collections.unmodifiableMap(orderedBones);
    var c = new LinkedHashMap<String, ClipId>();
    for (var x : model.clips()) c.put(x.id().value(), x.id());
    clips = Collections.unmodifiableMap(new LinkedHashMap<>(c));
  }

  public Result<BoneId> bone(String name) {
    if (name == null || name.isBlank())
      return Result.failure(
          Diagnostic.of(Severity.ERROR, DiagnosticCodes.CONFIG_BONE_MISSING, "blank bone name"));
    var x = bones.getOrDefault(name, List.of());
    if (x.isEmpty())
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR, DiagnosticCodes.CONFIG_BONE_MISSING, "bone not found: " + name));
    if (x.size() > 1) {
      var context = new TreeMap<String, String>();
      for (int i = 0; i < x.size(); i++) context.put("candidate." + i, x.get(i).value());
      return Result.failure(
          new Diagnostic(
              Severity.ERROR,
              new io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCode(
                  DiagnosticCodes.CONFIG_BONE_AMBIGUOUS),
              null,
              "ambiguous bone: " + name,
              "choose one of the listed candidates",
              name,
              null,
              context));
    }
    return Result.success(x.get(0));
  }

  public Result<ClipId> clip(String name) {
    if (name == null || name.isBlank())
      return Result.failure(
          Diagnostic.of(Severity.ERROR, DiagnosticCodes.CONFIG_CLIP_MISSING, "blank clip name"));
    var x = clips.get(name);
    return x == null
        ? Result.failure(Diagnostic.of(Severity.ERROR, DiagnosticCodes.CONFIG_CLIP_MISSING, name))
        : Result.success(x);
  }
}
