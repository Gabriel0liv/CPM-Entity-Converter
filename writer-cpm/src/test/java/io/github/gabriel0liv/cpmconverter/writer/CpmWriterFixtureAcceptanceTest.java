package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.github.gabriel0liv.cpmconverter.projection.CpmLogicalElementV1;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmWriterFixtureAcceptanceTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void goldenFixturesAAndC() throws Exception {
    for (var fixture : List.of("fixture-a-humanoid", "fixture-c-deep-hierarchy")) {
      var result = CpmWriterFixturePipeline.run(fixture);
      assertParity(result);
      var expectedConfig = Path.of("..", "test-fixtures", fixture, "expected", "cpm-config-v1.json");
      var expectedManifest = Path.of("..", "test-fixtures", fixture, "expected", "cpm-artifact-manifest.json");
      if (Boolean.getBoolean("generateWriterSnapshots")) {
        Files.write(expectedConfig, result.configJson());
        Files.writeString(expectedManifest, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest(result)) + "\n");
        System.out.println("updated " + expectedConfig + " and " + expectedManifest);
      }
      assertTrue(Files.exists(expectedConfig), fixture + " config golden missing");
      assertTrue(Files.exists(expectedManifest), fixture + " artifact manifest missing");
      assertArrayEquals(Files.readAllBytes(expectedConfig), result.configJson(), fixture);
      assertEquals(mapper.readTree(Files.readString(expectedManifest)), manifest(result), fixture);
      var second = CpmWriterFixturePipeline.run(fixture);
      assertArrayEquals(result.artifact().bytes(), second.artifact().bytes(), fixture);
    }
  }

  @Test
  void structuredSmokeFixturesBAndD() throws Exception {
    for (var fixture : List.of("fixture-b-neck", "fixture-d-quadruped")) {
      var result = CpmWriterFixturePipeline.run(fixture);
      assertParity(result);
      assertEquals(2, result.inspected().entries().size());
      assertEquals(6, mapper.readTree(result.configJson()).get("elements").size());
      assertArrayEquals(result.sourcePng(), result.persistedPng());
      assertArrayEquals(result.artifact().bytes(), CpmWriterFixturePipeline.run(fixture).artifact().bytes());
    }
  }

  private void assertParity(CpmWriterFixturePipeline.WriterFixtureResult result) throws Exception {
    var config = mapper.readTree(result.configJson());
    assertEquals(6, config.get("elements").size());
    assertEquals(result.identifiedProjection().storeIds().elementAssignments().size(), persistedCount(config));
    assertArrayEquals(result.sourcePng(), result.persistedPng());
    assertTrue(result.configJson()[result.configJson().length - 1] == '\n');
    var text = new String(result.configJson(), StandardCharsets.UTF_8);
    for (var forbidden : List.of("C:\\", "D:\\", "geometry.geo.json", "mapping.yaml", "SourceLocation", "CpmNodeKey")) assertFalse(text.contains(forbidden));
  }

  private int persistedCount(JsonNode config) { int n = 0; for (var root : config.get("elements")) n += childrenCount(root.get("children")); return n; }
  private int childrenCount(JsonNode children) { int n = children.size(); for (var child : children) n += childrenCount(child.get("children")); return n; }

  private ObjectNode manifest(CpmWriterFixturePipeline.WriterFixtureResult result) throws Exception {
    var root = mapper.createObjectNode(); root.put("artifactSha256", sha(result.artifact().bytes())); root.put("artifactSize", result.artifact().size());
    var entries = root.putArray("entries");
    for (var e : result.inspected().entries()) { var n = entries.addObject(); n.put("name", e.name()); n.put("method", "DEFLATED"); n.put("time", "1980-01-01T00:00:00"); n.put("uncompressedSize", e.contents().length); n.put("sha256", sha(e.contents())); }
    return root;
  }
  private static String sha(byte[] data) throws Exception { var digest = MessageDigest.getInstance("SHA-256"); var out = digest.digest(data); var b = new StringBuilder(); for (var x : out) b.append(String.format("%02x", x)); return b.toString(); }
}
