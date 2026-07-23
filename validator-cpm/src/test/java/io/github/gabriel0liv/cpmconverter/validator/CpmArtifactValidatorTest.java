package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

class CpmArtifactValidatorTest {
  private static byte[] zip(String config) throws IOException {
    ByteArrayOutputStream out=new ByteArrayOutputStream(); try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("config.json"));z.write(config.getBytes(StandardCharsets.UTF_8));z.closeEntry();} return out.toByteArray();
  }
  @Test void validatesMinimalVersionOneProject() throws Exception {
    var r=new CpmArtifactValidator().validate(zip("{\"version\":1,\"elements\":[]}"));
    assertTrue(r.success()); assertEquals(0,r.value().summary().elementCount());
  }
  @Test void rejectsMissingConfig() throws Exception { assertFalse(new CpmArtifactValidator().validate(new byte[]{1,2,3}).success()); }
  @Test void requestDefensivelyCopiesBytes() throws Exception { byte[] b=zip("{\"version\":1,\"elements\":[]}"); var q=new CpmArtifactValidationRequest(b,CpmArtifactLimits.defaults()); b[0]=0; assertNotEquals(0,q.artifactBytes()[0]); }
}
