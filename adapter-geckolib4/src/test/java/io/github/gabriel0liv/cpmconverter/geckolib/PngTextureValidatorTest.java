package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PngTextureValidatorTest {
  private static final byte[] ONE_BY_ONE =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @TempDir Path temp;

  @Test
  void validPngUsesLogicalSourceAndPreservesHash() throws Exception {
    Path p = temp.resolve("texture with space.PNG");
    Files.write(p, ONE_BY_ONE);
    byte[] before = Files.readAllBytes(p);
    var result =
        new PngTextureValidator()
            .validate(p, new SourcePath("fixtures/texture.png"), PngValidationRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals("fixtures/texture.png", result.value().source().value());
    assertArrayEquals(before, Files.readAllBytes(p));
    assertNotNull(result.value().sha256());
  }

  @Test
  void invalidSignaturePointsAtSignature() throws Exception {
    Path p = temp.resolve("bad.bin");
    Files.write(p, new byte[] {1, 2, 3});
    var result =
        new PngTextureValidator()
            .validate(p, new SourcePath("bad.png"), PngValidationRequest.defaults());
    assertFalse(result.success());
    var d = result.diagnostics().all().get(0);
    assertEquals("/signature", d.location().jsonPointer());
    assertEquals("bad.png", d.location().source().value());
  }

  @Test
  void maxBytesReportsStructuredContext() throws Exception {
    Path p = temp.resolve("x.png");
    Files.write(p, ONE_BY_ONE);
    var limits = new PngParserLimits(1, 100, 100, 1000);
    var result =
        new PngTextureValidator()
            .validate(p, new SourcePath("x.png"), new PngValidationRequest(limits));
    assertFalse(result.success());
    var d = result.diagnostics().all().get(0);
    assertEquals("INPUT_LIMIT_EXCEEDED", d.code().value());
    assertEquals("/", d.location().jsonPointer());
    assertEquals("maxBytes", d.context().get("limitName"));
    assertEquals("1", d.context().get("limit"));
    assertEquals(Long.toString(ONE_BY_ONE.length), d.context().get("observed"));
  }

  @Test
  void missingFileUsesRootPointerWithoutAbsolutePath() {
    Path p = temp.resolve("missing.png");
    var result =
        new PngTextureValidator()
            .validate(p, new SourcePath("logical/missing.png"), PngValidationRequest.defaults());
    assertFalse(result.success());
    var d = result.diagnostics().all().get(0);
    assertEquals("/", d.location().jsonPointer());
    assertFalse(d.location().source().value().contains(temp.toString()));
  }
}
