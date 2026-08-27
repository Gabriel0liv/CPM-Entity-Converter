package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProjectIoRoundTripTest {
  @Test
  void officialSaveAndReopenPreservesStaticSemantics() throws Exception {
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();
    ProjectIoHarness harness = new ProjectIoHarness();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact artifact = pipeline.generate(fixture);
      ProjectIoRoundTripResult result = harness.roundTrip(artifact.bytes());

      assertEquals(
          ProjectIoRoundTripResult.Status.PASS,
          result.status(),
          fixture + ": " + result.message());
      assertEquals(result.before().generatedStoreIds(), result.after().generatedStoreIds(), fixture);
      assertEquals(
          result.before().parentByGeneratedPath(), result.after().parentByGeneratedPath(), fixture);
      assertEquals(
          result.before().boxUvOriginsByGeneratedPath(),
          result.after().boxUvOriginsByGeneratedPath(),
          fixture);
    }
  }
}
