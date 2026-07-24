package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectIoLoadConformanceTest {
  @Test
  void fixturesAndS003HaveExpectedLoadResults() throws Exception {
    for (String fixture :
        new String[] {
          "fixture-a-humanoid",
          "fixture-b-neck",
          "fixture-c-deep-hierarchy",
          "fixture-d-quadruped",
          "M2",
          "M3",
          "M4",
          "M5"
        }) {
      var report = T304ConformanceTestSupport.project(fixture);
      assertTrue(report.get("success").getAsBoolean(), fixture + " must load in ProjectIO");
      assertTrue(report.get("rootCount").getAsInt() > 0, fixture);
    }
    assertFalse(T304ConformanceTestSupport.project("M0").get("success").getAsBoolean());
    assertFalse(T304ConformanceTestSupport.project("M1").get("success").getAsBoolean());
  }
}
