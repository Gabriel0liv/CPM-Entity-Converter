package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmFixtureSmokeTest {
  private static final Map<String, String> EXPECTED_SHA256 = Map.of(
      "fixture-b-neck", "4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9",
      "fixture-d-quadruped", "82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695");

  @Test
  void officialWriterAndValidatorSmokeBAndD() throws Exception {
    for (String fixture : List.of("fixture-b-neck", "fixture-d-quadruped")) {
      byte[] first = CpmOfficialFixtureFactory.write(fixture);
      byte[] second = CpmOfficialFixtureFactory.write(fixture);
      assertArrayEquals(first, second, fixture);
      var result = new CpmArtifactValidator().validate(first);
      assertTrue(result.success(), fixture + " " + result.diagnostics().all());
      assertTrue(result.value().summary().rootCount() > 0);
      assertTrue(result.value().summary().elementCount() > 0);
      assertEquals(EXPECTED_SHA256.get(fixture), sha(first), fixture);
      assertTrue(result.value().summary().canonical(), fixture);
      assertEquals(CpmValidationLayerStatus.PASS, result.value().summary().layers().get(CpmValidationLayer.CANONICALITY));
      assertEquals(CpmValidationLayerStatus.PASS, result.value().summary().layers().get(CpmValidationLayer.PROJECT_GRAPH));
    }
  }

  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
