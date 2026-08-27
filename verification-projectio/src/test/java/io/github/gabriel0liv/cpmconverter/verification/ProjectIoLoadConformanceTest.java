package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectIoLoadConformanceTest {
  @Test
  void currentFixturesAndS003ControlsMatchOfficialProjectIo() throws Exception {
    ProjectIoHarness harness = new ProjectIoHarness();
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      ProjectIoSnapshot snapshot = harness.load(pipeline.generate(fixture).bytes());
      if (!snapshot.loaded()) {
        System.err.println(
            fixture + ": " + snapshot.failureType() + ": " + snapshot.failureMessage());
      }
      assertTrue(snapshot.loaded(), fixture + ": " + snapshot.failureMessage());
      assertEquals(6, snapshot.rootCount(), fixture);
    }

    Path s003 = repoRoot().resolve("spikes/minimal-cpmproject/artifacts");
    for (String name : List.of("M2", "M3", "M4", "M5")) {
      assertTrue(harness.load(s003.resolve(name + ".cpmproject")).loaded(), name);
    }
    assertFalse(harness.load(s003.resolve("M0.cpmproject")).loaded(), "M0");
    assertFalse(harness.load(s003.resolve("M1.cpmproject")).loaded(), "M1");
  }

  private static Path repoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isDirectory(current.resolve("test-fixtures"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("repository root containing test-fixtures was not found");
  }
}
