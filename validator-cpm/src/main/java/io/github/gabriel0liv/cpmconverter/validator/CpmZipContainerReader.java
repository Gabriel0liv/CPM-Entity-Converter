package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class CpmZipContainerReader {
  public Result<CpmArtifactInventory> read(byte[] bytes, CpmArtifactLimits limits) {
    if(bytes==null||bytes.length>limits.maxArtifactBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"<artifact>","artifact size limit exceeded"));
    ArrayList<CpmArtifactEntry> out=new ArrayList<>(); Set<String> names=new HashSet<>(); long total=0;
    try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(bytes))){ ZipEntry e; int p=0; while((e=in.getNextEntry())!=null){ if(e.isDirectory()||!safe(e.getName(),limits.maxEntryNameLength())) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_UNSAFE,e.getName(),"unsafe entry")); if(!names.add(e.getName().toLowerCase(Locale.ROOT))) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_DUPLICATE,e.getName(),"duplicate entry")); ByteArrayOutputStream b=new ByteArrayOutputStream(); in.transferTo(b); total+=b.size(); if(b.size()>limits.maxEntryUncompressedBytes()||total>limits.maxTotalUncompressedBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,e.getName(),"uncompressed limit exceeded")); out.add(new CpmArtifactEntry(e.getName(),e.getMethod(),e.getTime(),e.getCompressedSize(),b.size(),e.getCrc(),false,p++)); } }
    catch(IOException ex){ return Result.failure(error(DiagnosticCodes.CPM_CONTAINER_INVALID,"<artifact>","invalid ZIP")); }
    if(names.stream().noneMatch("config.json"::equals)) return Result.failure(error(DiagnosticCodes.CPM_ENTRY_MISSING,"<artifact>","config.json is required"));
    return Result.success(new CpmArtifactInventory(out));
  }
  private static boolean safe(String n,int max){return n!=null&&!n.isBlank()&&n.length()<=max&&!n.startsWith("/")&&!n.contains("\\")&&!n.contains("..")&&!n.endsWith("/");}
  private static Diagnostic error(String code,String source,String message){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),SourceLocation.of(new SourcePath(source)),message,"repair the artifact",null,null,new TreeMap<>());}
}
