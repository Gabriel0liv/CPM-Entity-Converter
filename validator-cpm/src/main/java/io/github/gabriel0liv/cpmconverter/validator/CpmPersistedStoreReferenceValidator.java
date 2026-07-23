package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedStoreReferenceValidator {
  DiagnosticBag validate(CpmPersistedProjectV1 project) {
    var out = new DiagnosticBag();
    var targets = project.effectiveTargets();
    for (var e : project.elements()) if (e.storeId() >= 0 && !targets.containsKey(e.storeId())) out = out.add(err(e.pointer()+"/storeID"));
    return out;
  }
  private static Diagnostic err(String pointer){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_DANGLING_ANIMATION_REF),new SourceLocation(new SourcePath("config.json"),null,null,pointer,null),"effective target missing","repair target registry",null,null,new TreeMap<>());}
}
