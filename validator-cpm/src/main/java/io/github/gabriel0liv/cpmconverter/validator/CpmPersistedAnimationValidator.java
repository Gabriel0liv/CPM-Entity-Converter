package io.github.gabriel0liv.cpmconverter.validator;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;
final class CpmPersistedAnimationValidator {
  DiagnosticBag validate(List<CpmPersistedAnimationV1> animations, CpmPersistedProjectV1 project){var b=new DiagnosticBag(); for(var a:animations) for(var f:a.frames()) for(var c:f.components()) if(!project.effectiveTargets().containsKey(c.storeId())) {var ctx=new TreeMap<String,String>();ctx.put("entryName",a.entryName());ctx.put("frameIndex",Integer.toString(f.index()));ctx.put("componentIndex",Integer.toString(c.index()));ctx.put("storeID",Long.toString(c.storeId())); b=b.add(new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(DiagnosticCodes.CPM_DANGLING_ANIMATION_REF),new SourceLocation(new SourcePath(a.entryName()),null,null,pointer(f.pointer(),c.index()),null),"animation target does not exist","reference an existing root or element storeID",null,a.logicalName(),ctx));} return b; }
  private static String pointer(String base,int index){return (base==null?"/frames":base)+"/components/"+index+"/storeID";}
}
