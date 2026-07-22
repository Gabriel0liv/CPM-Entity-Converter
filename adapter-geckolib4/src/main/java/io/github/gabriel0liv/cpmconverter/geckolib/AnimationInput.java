package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.file.Path;

public record AnimationInput(Path path, SourcePath logicalSource) {
  public AnimationInput {
    if (path == null || logicalSource == null)
      throw new IllegalArgumentException("animation input");
  }
}
