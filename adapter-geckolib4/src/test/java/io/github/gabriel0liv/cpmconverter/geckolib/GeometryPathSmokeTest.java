package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeometryPathSmokeTest {
  private static final String JSON =
      "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"g\"},\"bones\":[{\"name\":\"body\"}]}]}";

  @Test
  void byteSourceNormalizesWindowsSeparatorsAndPointers() {
    var result =
        new GeckoGeometryParser()
            .parse(
                JSON.getBytes(),
                new SourcePath("dir\\fixture.geo.json"),
                GeometryParseRequest.defaults());
    assertTrue(result.success());
    assertEquals("dir/fixture.geo.json", result.value().source().value());
  }

  @Test
  void tempFilesWithSpacesAndUnicodeAreAccepted(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("dir é").resolve("fixture space.geo.json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, JSON);
    var result = new GeckoGeometryParser().parse(file, GeometryParseRequest.defaults());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertFalse(result.value().source().value().contains("\\"));
    assertFalse(result.value().source().value().matches("^[A-Za-z]:/.*"));
  }

  @Test
  void diagnosticsKeepPointerIndependentOfFilename() {
    String invalid =
        "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{}}]}";
    var first =
        new GeckoGeometryParser()
            .parse(
                invalid.getBytes(),
                new SourcePath("a space.geo.json"),
                GeometryParseRequest.defaults());
    var second =
        new GeckoGeometryParser()
            .parse(
                invalid.getBytes(),
                new SourcePath("unicode-é.geo.json"),
                GeometryParseRequest.defaults());
    assertEquals(
        first.diagnostics().errors().get(0).location().jsonPointer(),
        second.diagnostics().errors().get(0).location().jsonPointer());
    assertFalse(
        first.diagnostics().errors().get(0).location().source().value().matches("^[A-Za-z]:/.*"));
  }
}
