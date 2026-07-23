package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedUvTextureValidator {
  DiagnosticBag validate(JsonNode config, CpmPersistedSize2i grid) {
    var bag=new DiagnosticBag(); JsonNode roots=config.path("elements"); for(int r=0;r<roots.size();r++) bag=check(roots.get(r).path("children"),"/elements/"+r+"/children",grid,bag); return bag;
  }
  private DiagnosticBag check(JsonNode nodes,String pointer,CpmPersistedSize2i grid,DiagnosticBag bag){ if(!nodes.isArray()) return bag; for(int i=0;i<nodes.size();i++){JsonNode n=nodes.get(i); String p=pointer+"/"+i; if(n.path("texture").asBoolean(false)){ if(n.has("faceUV")){JsonNode faces=n.get("faceUV"); if(!faces.isObject()||faces.size()==0) bag=bag.add(error(DiagnosticCodes.UV_INVALID,p+"/faceUV","faceUV must be a non-empty object")); else for(var it=faces.fields();it.hasNext();){var e=it.next(); if(!Set.of("north","south","east","west","up","down").contains(e.getKey())) bag=bag.add(error(DiagnosticCodes.UV_FACE_UNKNOWN,p+"/faceUV/"+e.getKey(),"unknown UV face")); else if(!e.getValue().isObject()) bag=bag.add(error(DiagnosticCodes.UV_INVALID,p+"/faceUV/"+e.getKey(),"face UV must be object")); } } else {JsonNode u=n.get("u"),v=n.get("v"); if(u==null||!u.isIntegralNumber()) bag=bag.add(error(DiagnosticCodes.UV_INVALID,p+"/u","u must be integer")); if(v==null||!v.isIntegralNumber()) bag=bag.add(error(DiagnosticCodes.UV_INVALID,p+"/v","v must be integer")); } } bag=check(n.path("children"),p+"/children",grid,bag); } return bag; }
  private static Diagnostic error(String c,String p,String m){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(c),new SourceLocation(new SourcePath("config.json"),null,null,p,null),m,"repair the persisted UV",null,null,new TreeMap<>());}
}
