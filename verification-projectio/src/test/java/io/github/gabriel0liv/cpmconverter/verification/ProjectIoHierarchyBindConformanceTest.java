package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProjectIoHierarchyBindConformanceTest {
  @Test
  void generatedHierarchyAndBindTransformsMatchOfficialProjectIo() throws Exception {
    ProjectIoHarness harness = new ProjectIoHarness();
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact artifact = pipeline.generate(fixture);
      ExpectedStaticSnapshot expected =
          ExpectedStaticSnapshot.from(artifact.project(), artifact.storeIds());
      ProjectIoSnapshot loaded = harness.load(artifact.bytes());

      assertTrue(
          loaded.paths().stream().anyMatch(path -> path.contains("body/entity_root")), fixture);
      assertEquals(expected.parentByGeneratedPath(), loaded.parentByGeneratedPath(), fixture);

      Map<String, ProjectIoElementSnapshot> actualByPath =
          loaded.elements().stream()
              .collect(Collectors.toMap(ProjectIoElementSnapshot::path, Function.identity()));
      for (Map.Entry<String, ExpectedStaticSnapshot.Element> entry :
          expected.generatedElementsByPath().entrySet()) {
        ProjectIoElementSnapshot actual = actualByPath.get(entry.getKey());
        assertNotNull(actual, fixture + ": " + entry.getKey());
        ExpectedStaticSnapshot.Element expectedElement = entry.getValue();
        assertVec(expectedElement.position(), actual.position(), 1e-4, fixture + ": position");
        assertVec(
            expectedElement.rotationDegrees(), actual.rotation(), 1e-4, fixture + ": rotation");
        assertVec(expectedElement.scale(), actual.meshScale(), 1e-6, fixture + ": scale");
      }
    }
  }

  private static void assertVec(
      Vec3Snapshot expected, Vec3Snapshot actual, double tolerance, String message) {
    assertEquals(expected.x(), actual.x(), tolerance, message + ".x");
    assertEquals(expected.y(), actual.y(), tolerance, message + ".y");
    assertEquals(expected.z(), actual.z(), tolerance, message + ".z");
  }
}
