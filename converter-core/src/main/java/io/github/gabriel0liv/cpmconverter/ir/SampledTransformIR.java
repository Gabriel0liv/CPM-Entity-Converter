package io.github.gabriel0liv.cpmconverter.ir;

import io.github.gabriel0liv.cpmconverter.math.*;

public record SampledTransformIR(
    Vec3d translation, Quatd rotation, Vec3d scale, RotationContinuityIR rotationContinuity) {
  public SampledTransformIR {
    if (translation == null || rotation == null || scale == null || rotationContinuity == null)
      throw new IllegalArgumentException("sample");
  }

  public SampledTransformIR(Vec3d translation, Quatd rotation, Vec3d scale) {
    this(translation, rotation, scale, new RotationContinuityIR(true));
  }
}
