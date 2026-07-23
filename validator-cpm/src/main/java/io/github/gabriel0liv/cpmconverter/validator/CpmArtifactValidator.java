package io.github.gabriel0liv.cpmconverter.validator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Independent validator for the converter-supported CPM V1 persisted artifact. */
public final class CpmArtifactValidator {
  private static final List<CpmValidationLayer> ORDER = List.of(CpmValidationLayer.values());
  private final ObjectMapper mapper;
  public CpmArtifactValidator() {
    StreamReadConstraints limits = StreamReadConstraints.builder().maxNestingDepth(128).maxStringLength(1024 * 1024).maxNumberLength(128).build();
    JsonFactory factory = JsonFactory.builder().streamReadConstraints(limits).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    mapper = JsonMapper.builder(factory).build();
  }
  public Result<CpmValidatedArtifactV1> validate(byte[] bytes) { return validate(CpmArtifactValidationRequest.of(bytes)); }
  public Result<CpmValidatedArtifactV1> validate(CpmArtifactValidationRequest request) {
    DiagnosticBag bag = new DiagnosticBag();
    byte[] bytes;
    try { bytes = request.artifactBytes(); } catch (RuntimeException e) { return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID, "<artifact>", null, "invalid request", "provide artifact bytes")); }
    var statuses = new LinkedHashMap<CpmValidationLayer,CpmValidationLayerStatus>();
    for (var l : ORDER) statuses.put(l, CpmValidationLayerStatus.SKIPPED);
    if (bytes.length > request.limits().maxArtifactBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"<artifact>",null,"artifact size limit exceeded","reduce artifact size"));
    LinkedHashMap<String,byte[]> entries = new LinkedHashMap<>(); ArrayList<CpmArtifactEntry> inventory = new ArrayList<>();
    try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry e; Set<String> lower = new HashSet<>(); long total = 0; int pos = 0;
      while ((e = zin.getNextEntry()) != null) {
        String name=e.getName(); if (!safe(name, request.limits().maxEntryNameLength())) { bag=bag.add(error(DiagnosticCodes.CPM_ENTRY_UNSAFE,"<artifact>",null,"unsafe entry name","use a safe relative entry name")); continue; }
        if (!lower.add(name.toLowerCase(Locale.ROOT)) || entries.containsKey(name)) bag=bag.add(error(DiagnosticCodes.CPM_ENTRY_DUPLICATE,"<artifact>",null,"duplicate entry","remove duplicate entries"));
        if (e.isDirectory()) { bag=bag.add(error(DiagnosticCodes.CPM_CONTAINER_INVALID,"<artifact>",null,"directory entries are unsupported","store files only")); continue; }
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; long count=0;
        while((n=zin.read(buf))>=0){ count+=n; total+=n; if(count>request.limits().maxEntryUncompressedBytes()||total>request.limits().maxTotalUncompressedBytes()) { bag=bag.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"<artifact>",null,"uncompressed size limit exceeded","reduce entry size")); break; } out.write(buf,0,n); }
        byte[] data=out.toByteArray(); entries.put(name,data); inventory.add(new CpmArtifactEntry(name,e.getMethod(),e.getTime(),e.getCompressedSize(),data.length,e.getCrc(),false,pos++));
      }
    } catch (IOException ex) { bag=bag.add(error(DiagnosticCodes.CPM_CONTAINER_INVALID,"<artifact>",null,"invalid ZIP container","provide a readable ZIP artifact")); }
    statuses.put(CpmValidationLayer.CONTAINER, bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS);
    if (!entries.containsKey("config.json")) bag=bag.add(error(DiagnosticCodes.CPM_ENTRY_MISSING,"<artifact>","/config.json","config.json is required","add config.json"));
    for (String name : entries.keySet()) {
      if (name.equals("config.json") || name.equals("skin.png")) continue;
      if (!name.startsWith("animations/") || !name.endsWith(".json") || name.substring("animations/".length()).contains("/"))
        bag=bag.add(error(DiagnosticCodes.CPM_FEATURE_UNSUPPORTED,"<artifact>",null,"unsupported artifact entry: "+name,"remove the unsupported entry"));
    }
    JsonNode config=null; boolean canonical=true;
    if (entries.containsKey("config.json")) {
      try { config=mapper.readTree(entries.get("config.json")); if(config==null||!config.isObject()) throw new IOException("root must be object"); statuses.put(CpmValidationLayer.CONFIG_SYNTAX,CpmValidationLayerStatus.PASS); }
      catch(Exception ex){ bag=bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,"config.json",null,"invalid JSON: "+ex.getMessage(),"provide strict JSON")); statuses.put(CpmValidationLayer.CONFIG_SYNTAX,CpmValidationLayerStatus.FAIL); }
    }
    if(config!=null){
      JsonNode v=config.get("version"); if(v==null||!v.isInt()||v.intValue()!=1) bag=bag.add(error(DiagnosticCodes.CPM_UNSUPPORTED_VERSION,"config.json","/version","CPM version must be 1","use version 1"));
      JsonNode elements=config.get("elements"); if(elements==null||!elements.isArray()) bag=bag.add(error(DiagnosticCodes.CPM_CONFIG_INVALID,"config.json","/elements","elements array is required","add elements"));
      else if(elements.size()>request.limits().maxElements()) bag=bag.add(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"config.json","/elements","element limit exceeded","reduce elements"));
      statuses.put(CpmValidationLayer.CONFIG_SCHEMA, bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS);
      canonical = isCanonicalConfig(entries.get("config.json"));
      if(!canonical) bag=bag.add(warning(DiagnosticCodes.CPM_NON_CANONICAL,"config.json",null,"encoding is semantically valid but non-canonical","use canonical writer encoding"));
    }
    int roots=0,elements=0,storeIds=0,textured=0; int tw=0,th=0;
    if(config!=null && config.path("elements").isArray()) {
      elements=config.path("elements").size(); Set<Long> ids=new HashSet<>();
      for(JsonNode n:config.path("elements")){ if(n.has("storeID")){ if(!n.path("storeID").canConvertToLong()||!n.path("storeID").isIntegralNumber()) bag=bag.add(error(DiagnosticCodes.CPM_INVALID_STORE_ID,"config.json","/elements/storeID","invalid storeID","use a safe integer")); else if(!ids.add(n.path("storeID").longValue())) bag=bag.add(error(DiagnosticCodes.CPM_DUPLICATE_STORE_ID,"config.json","/elements/storeID","duplicate storeID","use unique storeIDs")); else storeIds++; } if(n.has("texture")) textured++; }
      Set<String> rootNames=Set.of("head","body","left_arm","right_arm","left_leg","right_leg");
      roots=0; for(JsonNode n:config.path("elements")) if(rootNames.contains(n.path("id").asText())) roots++;
    }
    if(entries.containsKey("skin.png")){ byte[] png=entries.get("skin.png"); if(!isPng(png)) bag=bag.add(error(DiagnosticCodes.PNG_INVALID,"skin.png","/signature","invalid PNG","provide a PNG texture")); else { tw=readInt(png,16); th=readInt(png,20); } }
    statuses.put(CpmValidationLayer.PROJECT_GRAPH, bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS);
    statuses.put(CpmValidationLayer.STORE_REFERENCES, bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS);
    statuses.put(CpmValidationLayer.UV_TEXTURE, bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS);
    int animationCount=0;
    for (var entry: entries.entrySet()) if (entry.getKey().startsWith("animations/")) {
      try { JsonNode animation=mapper.readTree(entry.getValue()); if(animation==null||!animation.isObject()) throw new IOException("root must be object"); animationCount++; }
      catch(Exception ex){ bag=bag.add(error(DiagnosticCodes.CPM_ANIMATION_INVALID,entry.getKey(),null,"invalid animation JSON","provide a supported animation object")); }
    }
    statuses.put(CpmValidationLayer.ANIMATIONS,bag.hasErrors()?CpmValidationLayerStatus.FAIL:CpmValidationLayerStatus.PASS); statuses.put(CpmValidationLayer.CANONICALITY,canonical?CpmValidationLayerStatus.PASS:CpmValidationLayerStatus.WARN);
    if(bag.hasErrors()) return Result.failure(bag);
    var summary=new CpmValidationSummary(statuses,canonical,roots,elements,storeIds,textured,animationCount,0,0,entries.containsKey("skin.png"),tw,th);
    return Result.success(new CpmValidatedArtifactV1(new CpmPersistedProjectV1(config),List.of(),new CpmArtifactInventory(inventory),summary),bag);
  }
  private static boolean safe(String n,int max){ return n!=null&&!n.isBlank()&&n.length()<=max&&!n.contains("\\")&&!n.startsWith("/")&&!n.contains("\0")&&!Arrays.asList(n.split("/",-1)).contains("..")&&!n.matches("^[A-Za-z]:.*")&&!n.endsWith("/"); }
  private static boolean isPng(byte[] b){return b.length>=24&& (b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&readInt(b,12)==0x49484452;}
  private static int readInt(byte[] b,int p){return ((b[p]&255)<<24)|((b[p+1]&255)<<16)|((b[p+2]&255)<<8)|(b[p+3]&255);}
  private boolean isCanonicalConfig(byte[] b){String s=new String(b,StandardCharsets.UTF_8); return s.endsWith("\n")&&!s.contains("\r")&&!s.contains("\n  ");}
  private static Diagnostic error(String code,String source,String pointer,String message,String suggestion){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath(source),null,null,pointer,null),message,suggestion,null,null,new TreeMap<>());}
  private static Diagnostic warning(String code,String source,String pointer,String message,String suggestion){return new Diagnostic(Severity.WARNING,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath(source),null,null,pointer,null),message,suggestion,null,null,new TreeMap<>());}
}
