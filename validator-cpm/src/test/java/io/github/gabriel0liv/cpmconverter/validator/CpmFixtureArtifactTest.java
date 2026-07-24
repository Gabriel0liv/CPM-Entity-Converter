package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmFixtureArtifactTest {
  @Test
  void officialWriterArtifactsAAndCAreValidatedDeterministically() throws Exception {
    for (String fixture : List.of("fixture-a-humanoid", "fixture-c-deep-hierarchy")) {
      byte[] first = CpmOfficialFixtureFactory.write(fixture);
      byte[] second = CpmOfficialFixtureFactory.write(fixture);
      assertArrayEquals(first, second, fixture);
      var result = new CpmArtifactValidator().validate(first);
      assertTrue(result.success(), fixture + " " + result.diagnostics().all());
      assertEquals(result.value().summary(), new CpmArtifactValidator().validate(second).value().summary());
      assertTrue(sha(first).length() == 64);
      assertTrue(result.value().summary().rootCount() > 0);
      assertTrue(result.value().summary().elementCount() > 0);
      assertTrue(result.value().summary().texturedElementCount() > 0);
    }
  }

  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
