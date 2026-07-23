package io.github.gabriel0liv.cpmconverter.validator;

import java.util.*;

final class CpmValidationLayerTracker {
  private final EnumMap<CpmValidationLayer,CpmValidationLayerStatus> values=new EnumMap<>(CpmValidationLayer.class);
  CpmValidationLayerTracker(){for(var layer:CpmValidationLayer.values()) values.put(layer,CpmValidationLayerStatus.SKIPPED);}
  void pass(CpmValidationLayer l){values.put(l,CpmValidationLayerStatus.PASS);}
  void warn(CpmValidationLayer l){values.put(l,CpmValidationLayerStatus.WARN);}
  void fail(CpmValidationLayer l){values.put(l,CpmValidationLayerStatus.FAIL);}
  CpmValidationLayerStatus status(CpmValidationLayer l){return values.get(l);}
  Map<CpmValidationLayer,CpmValidationLayerStatus> snapshot(){return Collections.unmodifiableMap(new LinkedHashMap<>(values));}
}
