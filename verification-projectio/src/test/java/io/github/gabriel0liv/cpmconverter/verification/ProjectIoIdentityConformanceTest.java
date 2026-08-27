package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.cpm.CpmElementV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmRootV1;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ProjectIoIdentityConformanceTest {
  @Test
  void generatedStoreIdsAndPathsSurviveOfficialProjectIoLoad() throws Exception {
    ProjectIoHarness harness = new ProjectIoHarness();
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact artifact = pipeline.generate(fixture);
      ProjectIoSnapshot loaded = harness.load(artifact.bytes());

      List<Long> expectedIds = new ArrayList<>();
      List<String> expectedPaths = new ArrayList<>();
      for (CpmRootV1 root : artifact.project().roots()) {
        String rootPath = root.vanillaPart().name().toLowerCase(Locale.ROOT);
        for (CpmElementV1 child : root.children()) {
          appendExpected(artifact, child, rootPath, expectedIds, expectedPaths);
        }
      }

      assertEquals(expectedIds, loaded.generatedStoreIds(), fixture);
      assertEquals(expectedPaths, loaded.generatedPaths(), fixture);
      assertTrue(
          loaded.generatedStoreIds().stream().allMatch(id -> id >= 1000 && id <= 9007199254740991L),
          fixture);
      assertEquals(
          loaded.generatedStoreIds().size(),
          new HashSet<>(loaded.generatedStoreIds()).size(),
          fixture);
    }
  }

  @Test
  void s003IdentityControlsRemainObservable() {
    ProjectIoHarness harness = new ProjectIoHarness();
    Path s003 = repoRoot().resolve("spikes/minimal-cpmproject/artifacts");

    ProjectIoSnapshot m3 = harness.load(s003.resolve("M3.cpmproject"));
    assertTrue(m3.storeIds().contains(1000L));

    ProjectIoSnapshot m5 = harness.load(s003.resolve("M5.cpmproject"));
    assertTrue(m5.storeIds().contains(1000L));
    assertTrue(m5.animationReferenceCount() > 0);
  }

  private static void appendExpected(
      CurrentFixtureArtifact artifact,
      CpmElementV1 element,
      String parentPath,
      List<Long> ids,
      List<String> paths) {
    String path = parentPath + "/" + element.name();
    ids.add(artifact.storeIds().elementId(element.key()));
    paths.add(path);
    for (CpmElementV1 child : element.children()) {
      appendExpected(artifact, child, path, ids, paths);
    }
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
