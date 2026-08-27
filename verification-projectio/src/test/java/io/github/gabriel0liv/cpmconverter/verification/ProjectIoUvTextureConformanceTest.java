package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectIoUvTextureConformanceTest {
  @Test
  void generatedUvAndTextureStateMatchesOfficialProjectIo() throws Exception {
    ProjectIoHarness harness = new ProjectIoHarness();
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact artifact = pipeline.generate(fixture);
      ExpectedStaticSnapshot expected =
          ExpectedStaticSnapshot.from(artifact.project(), artifact.storeIds());
      ProjectIoSnapshot loaded = harness.load(artifact.bytes());

      assertTrue(loaded.elements().stream().anyMatch(ProjectIoElementSnapshot::texture), fixture);
      assertTrue(
          loaded.elements().stream()
              .filter(ProjectIoElementSnapshot::texture)
              .allMatch(element -> element.textureSize() > 0),
          fixture);
      assertEquals(expected.boxUvOriginsByPath(), loaded.boxUvOriginsByGeneratedPath(), fixture);
      assertEquals(
          expected.perFaceUvPresenceByPath(), loaded.perFaceUvPresenceByGeneratedPath(), fixture);
      assertEquals(expected.perFaceUvByPath(), loaded.perFaceUvByGeneratedPath(), fixture);

      if (fixture.equals("fixture-c-deep-hierarchy")) {
        assertFalse(expected.perFaceUvByPath().isEmpty(), fixture);
        assertTrue(
            expected.perFaceUvByPath().values().stream().anyMatch(faces -> faces.size() == 6),
            fixture);
      }
    }
  }
}
