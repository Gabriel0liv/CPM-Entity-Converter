package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectIoRoundTripTest {
  @Test
  void roundTripStatusIsExplicitForEveryFixture() throws Exception {
    for (String fixture :
        new String[] {
          "fixture-a-humanoid", "fixture-b-neck", "fixture-c-deep-hierarchy", "fixture-d-quadruped"
        }) {
      var report = T304ConformanceTestSupport.project(fixture);
      assertTrue(report.has("roundTrip"), fixture);
      assertTrue(
          report.get("roundTrip").getAsString().equals("PASS")
              || report.get("roundTrip").getAsString().equals("NOT_AVAILABLE"),
          fixture);
    }
  }
}
