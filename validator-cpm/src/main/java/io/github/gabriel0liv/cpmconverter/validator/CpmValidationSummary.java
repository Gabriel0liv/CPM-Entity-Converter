package io.github.gabriel0liv.cpmconverter.validator;

import java.util.*;

public record CpmValidationSummary(Map<CpmValidationLayer,CpmValidationLayerStatus> layers, boolean canonical,
    int rootCount, int elementCount, int generatedStoreIdCount, int texturedElementCount, int animationCount,
    int frameCount, int componentReferenceCount, boolean texturePresent, int textureWidth, int textureHeight) {
  public CpmValidationSummary { layers = Collections.unmodifiableMap(new LinkedHashMap<>(layers)); }
}
