package io.github.gabriel0liv.cpmconverter.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.config.MappingLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** NON_PRODUCTION: executes the fixture contract audit used by CI. */
class FixtureManifestTest {
  private static final List<String> FIXTURES =
      List.of(
          "fixture-a-humanoid",
          "fixture-b-neck",
          "fixture-c-deep-hierarchy",
          "fixture-d-quadruped");

  @Test
  void manifestAuditExecutesAllContracts() throws Exception {
    Path root = fixtureRoot();
    Process process =
        new ProcessBuilder("python", "scripts/manifest.py", "--check")
            .directory(root.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    assertEquals(0, process.waitFor(), output);
    assertTrue(output.contains("4 fixtures verified"), output);

    ObjectMapper mapper = new ObjectMapper();
    for (String fixture : FIXTURES) {
      Path directory = root.resolve(fixture);
      assertRequiredContracts(directory);
      JsonNode inventory = mapper.readTree(directory.resolve("expected/inventory.json").toFile());
      JsonNode compiled =
          mapper.readTree(directory.resolve("expected/mapping-compiled.json").toFile());
      JsonNode diagnostics =
          mapper.readTree(directory.resolve("expected/diagnostics.json").toFile());
      JsonNode invariants = mapper.readTree(directory.resolve("expected/invariants.json").toFile());
      assertEquals(fixture, inventory.get("fixture").asText());
      assertTrue(compiled.has("boneIds"), "compiled mapping must contain resolved IDs");
      assertTrue(compiled.has("stateMappings"), "compiled mapping must contain states");
      assertTrue(diagnostics.has("expected"));
      assertTrue(invariants.path("acyclic").asBoolean());
      assertTrue(invariants.path("sourceOrderPreserved").asBoolean());
    }
  }

  @Test
  void mappingsAreValidatedByTheVersionedSchemaBeforeBinding() throws Exception {
    Path root = fixtureRoot();
    MappingLoader loader = new MappingLoader();
    for (String fixture : FIXTURES) {
      var result = loader.load(root.resolve(fixture).resolve("mapping.yaml"));
      assertTrue(result.success(), fixture + ": " + result.diagnostics());
      assertEquals(1, result.value().schemaVersion());
    }
  }

  private static void assertRequiredContracts(Path directory) {
    for (String relative :
        List.of(
            "README.md",
            "PROVENANCE.md",
            "geometry.geo.json",
            "animations.animation.json",
            "mapping.yaml",
            "texture.png",
            "expected/inventory.json",
            "expected/mapping-compiled.json",
            "expected/diagnostics.json",
            "expected/invariants.json")) {
      assertTrue(Files.isRegularFile(directory.resolve(relative)), directory + ": " + relative);
    }
  }

  private static Path fixtureRoot() throws IOException {
    Path current = Path.of(".").toAbsolutePath().normalize();
    if (Files.isDirectory(current.resolve("fixture-a-humanoid"))) {
      return current;
    }
    Path parent = current.resolve("..").normalize();
    if (Files.isDirectory(parent.resolve("fixture-a-humanoid"))) {
      return parent;
    }
    throw new IOException("cannot locate test-fixtures root from " + current);
  }
}
