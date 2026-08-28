package io.github.gabriel0liv.cpmconverter.sampling;

import io.github.gabriel0liv.cpmconverter.ir.TransformMode;
import io.github.gabriel0liv.cpmconverter.ir.TransformSpace;

public record TrackSemanticsIR(TransformMode mode, TransformSpace space) {
  public TrackSemanticsIR {
    if (mode == null || space == null) throw new IllegalArgumentException("track semantics");
  }
}
