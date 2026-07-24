package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmFixtureArtifactTest {
  private static final Map<String, String> EXPECTED_SHA256 = Map.of(
      "fixture-a-humanoid", "31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97",
      "fixture-c-deep-hierarchy", "177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3");

  @Test
  void officialWriterArtifactsAAndCAreValidatedDeterministically() throws Exception {
    for (String fixture : List.of("fixture-a-humanoid", "fixture-c-deep-hierarchy")) {
      byte[] first = CpmOfficialFixtureFactory.write(fixture);
      byte[] second = CpmOfficialFixtureFactory.write(fixture);
      assertArrayEquals(first, second, fixture);
      var result = new CpmArtifactValidator().validate(first);
      assertTrue(result.success(), fixture + " " + result.diagnostics().all());
      assertEquals(result.value().summary(), new CpmArtifactValidator().validate(second).value().summary());
      assertEquals(EXPECTED_SHA256.get(fixture), sha(first), fixture);
      assertTrue(result.value().summary().canonical(), fixture);
      assertEquals(CpmValidationLayerStatus.PASS, result.value().summary().layers().get(CpmValidationLayer.CANONICALITY));
      assertEquals(CpmValidationLayerStatus.PASS, result.value().summary().layers().get(CpmValidationLayer.PROJECT_GRAPH));
      assertEquals(CpmValidationLayerStatus.PASS, result.value().summary().layers().get(CpmValidationLayer.UV_TEXTURE));
      assertTrue(result.value().summary().texturePresent(), fixture);
      assertTrue(result.value().summary().textureWidth() > 0, fixture);
      assertTrue(result.value().summary().textureHeight() > 0, fixture);
      assertTrue(result.value().summary().generatedStoreIdCount() > 0, fixture);
      assertTrue(result.value().summary().rootCount() > 0);
      assertTrue(result.value().summary().elementCount() > 0);
      assertTrue(result.value().summary().texturedElementCount() > 0);
    }
  }

  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
