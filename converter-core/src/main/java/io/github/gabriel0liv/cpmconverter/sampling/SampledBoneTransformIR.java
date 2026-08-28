package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.SampledTransformIR;
import java.util.Optional;

public record SampledBoneTransformIR(
    BoneId bone, SampledTransformIR transform, Optional<TrackSemanticsIR> trackSemantics) {
  public SampledBoneTransformIR {
    if (bone == null || transform == null || trackSemantics == null) {
      throw new IllegalArgumentException("sampled bone transform");
    }
  }
}
