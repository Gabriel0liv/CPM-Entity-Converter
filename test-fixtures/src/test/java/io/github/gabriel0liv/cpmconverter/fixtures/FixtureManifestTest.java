package io.github.gabriel0liv.cpmconverter.fixtures;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;

class FixtureManifestTest {
  @Test
  void requiredFilesAndHashes() {
    Path root = Files.exists(Paths.get("fixture-a-humanoid")) ? Paths.get(".") : Paths.get("..");
    for (String n :
        List.of(
            "fixture-a-humanoid",
            "fixture-b-neck",
            "fixture-c-deep-hierarchy",
            "fixture-d-quadruped")) {
      Path d = root.resolve(n);
      assertTrue(Files.exists(d.resolve("README.md")));
      assertTrue(Files.exists(d.resolve("PROVENANCE.md")));
      assertTrue(Files.exists(d.resolve("geometry.geo.json")));
      assertTrue(Files.exists(d.resolve("animations.animation.json")));
      assertTrue(Files.exists(d.resolve("texture.png")));
      assertTrue(Files.exists(d.resolve("mapping.yaml")));
    }
  }
}
