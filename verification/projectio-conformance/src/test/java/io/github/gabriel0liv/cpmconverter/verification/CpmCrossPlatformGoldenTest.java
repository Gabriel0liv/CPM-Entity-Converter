package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmCrossPlatformGoldenTest {
  @Test
  void reportCarriesPinnedCrossPlatformHashes() throws Exception {
    assertEquals(
        "31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97",
        T304ConformanceTestSupport.project("fixture-a-humanoid").get("sha256").getAsString());
    assertEquals(
        "4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9",
        T304ConformanceTestSupport.project("fixture-b-neck").get("sha256").getAsString());
    assertEquals(
        "177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3",
        T304ConformanceTestSupport.project("fixture-c-deep-hierarchy").get("sha256").getAsString());
    assertEquals(
        "82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695",
        T304ConformanceTestSupport.project("fixture-d-quadruped").get("sha256").getAsString());
  }
}
