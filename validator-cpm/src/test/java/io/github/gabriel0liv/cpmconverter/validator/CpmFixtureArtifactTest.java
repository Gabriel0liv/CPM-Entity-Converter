package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

class CpmFixtureArtifactTest {
  private static byte[] artifact(String fixture) throws IOException {
    Path root=Files.exists(Path.of("test-fixtures",fixture))?Path.of("test-fixtures",fixture):Path.of("..","test-fixtures",fixture); byte[] config=Files.readAllBytes(root.resolve("expected/cpm-config-v1.json")); byte[] skin=Files.readAllBytes(root.resolve("texture.png")); ByteArrayOutputStream out=new ByteArrayOutputStream(); try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("config.json"));z.write(config);z.closeEntry();z.putNextEntry(new ZipEntry("skin.png"));z.write(skin);z.closeEntry();} return out.toByteArray();
  }
  @Test void validatesAvailableT302GoldenArtifacts() throws Exception { var v=new CpmArtifactValidator(); for(String f:new String[]{"fixture-a-humanoid","fixture-c-deep-hierarchy"}) { var r=v.validate(artifact(f)); assertTrue(r.success(),f+" "+r.diagnostics().all()); assertEquals(2,r.value().inventory().entries().size()); assertTrue(r.value().summary().texturePresent()); } }
}
