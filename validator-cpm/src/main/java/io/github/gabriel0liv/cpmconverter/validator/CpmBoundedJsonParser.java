package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

public final class CpmBoundedJsonParser {
  private final ObjectMapper mapper;
  public CpmBoundedJsonParser(CpmArtifactLimits limits) {
    JsonFactory f=JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(limits.maxJsonDepth()).maxStringLength(limits.maxStringLength()).maxNumberLength(limits.maxNumberLength()).build()).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build(); mapper=JsonMapper.builder(f).build();
  }
  public Result<JsonNode> parse(byte[] bytes, String entry) {
    try { JsonNode n=mapper.readTree(bytes); if(n==null||!n.isObject()) return Result.failure(diagnostic(DiagnosticCodes.CPM_CONFIG_INVALID,entry,"/","root must be object")); return Result.success(n); }
    catch(Exception e){ return Result.failure(diagnostic(DiagnosticCodes.CPM_CONFIG_INVALID,entry,null,e.getMessage()==null?"invalid JSON":e.getMessage())); }
  }
  private static Diagnostic diagnostic(String code,String source,String pointer,String message){ return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath(source),null,null,pointer,null),message,"provide strict JSON",null,null,new TreeMap<>()); }
}
