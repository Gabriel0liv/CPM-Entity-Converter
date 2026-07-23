package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedProjectValidator {
  DiagnosticBag validate(CpmPersistedProjectV1 project, CpmArtifactLimits limits) {
    var out = new DiagnosticBag();
    if (project == null) return out.add(err(DiagnosticCodes.CPM_CONFIG_INVALID, null, "project is null"));
    if (project.elements().size() > limits.maxElements()) out = out.add(err(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, "/elements", "element limit exceeded"));
    var ids = new HashSet<Long>();
    for (var e : project.elements()) {
      if (e.storeId() == null || e.storeId() <= 6 || e.storeId() > 9_007_199_254_740_991L) out = out.add(err(DiagnosticCodes.CPM_INVALID_STORE_ID, e.pointer()+"/storeID", "invalid storeID"));
      else if (!ids.add(e.storeId())) out = out.add(err(DiagnosticCodes.CPM_DUPLICATE_STORE_ID, e.pointer()+"/storeID", "duplicate storeID"));
      if (e.depth() > limits.maxElementDepth()) out = out.add(err(DiagnosticCodes.INPUT_LIMIT_EXCEEDED, e.pointer(), "element depth limit exceeded"));
    }
    return out;
  }
  private static Diagnostic err(String code,String pointer,String message){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath("config.json"),null,null,pointer,null),message,"fix the persisted project",null,null,new TreeMap<>());}
}
