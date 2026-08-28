package io.github.gabriel0liv.cpmconverter.sampling;

import java.util.List;

public record SampledFrameIR(int index, double timeSeconds, List<SampledBoneTransformIR> bones) {
  public SampledFrameIR {
    if (index < 0 || !Double.isFinite(timeSeconds) || timeSeconds < 0 || bones == null) {
      throw new IllegalArgumentException("sampled frame");
    }
    bones = List.copyOf(bones);
  }
}
