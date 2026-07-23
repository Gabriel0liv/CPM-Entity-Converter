package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;
import java.util.zip.CRC32;

public final class CpmPngValidator {
  private static final byte[] SIG={(byte)137,80,78,71,13,10,26,10};
  public Result<CpmPngMetadata> validate(byte[] bytes,CpmArtifactLimits limits){
    if (limits == null) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/","PNG limits are required"));
    if (bytes == null) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/","PNG bytes are null"));
    if(bytes.length>limits.maxSkinBytes()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"/","skin size limit exceeded"));
    if(bytes.length<24||!Arrays.equals(Arrays.copyOf(bytes,8),SIG)) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/signature","invalid PNG signature"));
    int p=8,count=0,width=0,height=0,depth=0,color=0; boolean ihdr=false,idat=false,iend=false;
    while(p+12<=bytes.length){ if(++count>limits.maxPngChunks()) return Result.failure(error(DiagnosticCodes.INPUT_LIMIT_EXCEEDED,"/","PNG chunk limit exceeded")); int len=read(bytes,p); if(len<0||p+12L+len>bytes.length) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/chunk","truncated PNG chunk")); String type=new String(bytes,p+4,4,java.nio.charset.StandardCharsets.US_ASCII); long crc=readUnsigned(bytes,p+8+len); CRC32 c=new CRC32(); c.update(bytes,p+4,4+len); if(c.getValue()!=crc) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/"+type,"PNG CRC mismatch"));
      if(type.equals("IHDR")){ if(ihdr||len!=13||p!=8) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/IHDR","invalid IHDR")); ihdr=true;width=read(bytes,p+8);height=read(bytes,p+12); depth=bytes[p+16]&255;color=bytes[p+17]&255; if(width<=0||height<=0||!validDepth(depth,color)||bytes[p+18]!=0||bytes[p+19]!=0||(bytes[p+20]&255)>1) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/IHDR","invalid PNG dimensions or format")); }
      else if(type.equals("IDAT")){ if(!ihdr||iend) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/IDAT","invalid IDAT position")); idat=true; } else if(type.equals("IEND")){ if(iend||len!=0||p+12!=bytes.length) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/IEND","invalid IEND")); iend=true; }
      p+=12+len; }
    if(!ihdr||!idat||!iend) return Result.failure(error(DiagnosticCodes.PNG_INVALID,"/","PNG requires IHDR, IDAT and IEND")); return Result.success(new CpmPngMetadata(width,height,depth,color,count));
  }
  private static boolean validDepth(int d,int c){return c==0?d==1||d==2||d==4||d==8||d==16:c==2||c==4||c==6?d==8||d==16:c==3?d==1||d==2||d==4||d==8:false;}
  private static int read(byte[] b,int p){return (b[p]&255)<<24|(b[p+1]&255)<<16|(b[p+2]&255)<<8|(b[p+3]&255);}
  private static long readUnsigned(byte[] b,int p){return Integer.toUnsignedLong(read(b,p));}
  private static Diagnostic error(String code,String ptr,String msg){return new Diagnostic(Severity.ERROR,DiagnosticCode.fromCatalog(code),new SourceLocation(new SourcePath("skin.png"),null,null,ptr,null),msg,"provide a structurally valid PNG",null,null,new TreeMap<>());}
}
