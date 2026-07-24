package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectIoIdentityConformanceTest {
  @Test
  void persistedIdsAndM5ReferencesAreObservedByProjectIo() throws Exception {
    for (String fixture :
        new String[] {
          "fixture-a-humanoid", "fixture-b-neck", "fixture-c-deep-hierarchy", "fixture-d-quadruped"
        }) {
      var report = T304ConformanceTestSupport.project(fixture);
      assertTrue(report.get("success").getAsBoolean(), fixture);
      assertTrue(report.getAsJsonArray("elements").size() > 0, fixture);
    }
    assertTrue(T304ConformanceTestSupport.project("M3").get("containsStoreId1000").getAsBoolean());
    var m5 = T304ConformanceTestSupport.project("M5");
    assertTrue(m5.get("containsStoreId1000").getAsBoolean());
    assertTrue(m5.get("animationReferenceCount").getAsInt() > 0);
  }
}
