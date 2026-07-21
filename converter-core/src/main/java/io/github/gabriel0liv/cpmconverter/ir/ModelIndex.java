package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import java.util.*;

public final class ModelIndex {
  private final Map<String, List<BoneId>> bones;
  private final Map<String, ClipId> clips;

  public ModelIndex(ModelIR model) {
    var b = new LinkedHashMap<String, List<BoneId>>();
    for (var x : model.bones()) b.computeIfAbsent(x.name(), k -> new ArrayList<>()).add(x.id());
    bones =
        b.entrySet().stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    var c = new LinkedHashMap<String, ClipId>();
    for (var x : model.clips()) c.put(x.id().value(), x.id());
    clips = Map.copyOf(c);
  }

  public Result<BoneId> bone(String name) {
    var x = bones.getOrDefault(name, List.of());
    if (x.size() != 1)
      return Result.failure(
          Diagnostic.of(
              Severity.ERROR,
              x.isEmpty()
                  ? DiagnosticCodes.CONFIG_BONE_MISSING
                  : DiagnosticCodes.CONFIG_BONE_AMBIGUOUS,
              name));
    return Result.success(x.get(0));
  }

  public Result<ClipId> clip(String name) {
    var x = clips.get(name);
    return x == null
        ? Result.failure(Diagnostic.of(Severity.ERROR, DiagnosticCodes.CONFIG_CLIP_MISSING, name))
        : Result.success(x);
  }
}
