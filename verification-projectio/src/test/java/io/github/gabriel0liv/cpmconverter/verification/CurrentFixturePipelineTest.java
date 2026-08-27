package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class CurrentFixturePipelineTest {
  private final CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

  @Test
  void generatesAndValidatesAllStaticFixturesDeterministically() throws Exception {
    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact first = pipeline.generate(fixture);
      CurrentFixtureArtifact second = pipeline.generate(fixture);

      assertArrayEquals(first.bytes(), second.bytes(), fixture);
      assertEquals(first.sha256(), second.sha256(), fixture);
      assertEquals(6, first.project().roots().size(), fixture);
      assertFalse(first.storeIds().elementIds().isEmpty(), fixture);
    }
  }
}
