package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class CpmArtifactMutationMatrixTest {
  @Test
  void focusedMutationsProduceExpectedOutcomes() throws Exception {
    var validator = new CpmArtifactValidator();
    var cases = new Object[][] {
      {"trailing-json", "{\"version\":1,\"elements\":[]}x", false, "CPM_CONFIG_INVALID"},
      {"unknown-field", "{\"version\":1,\"elements\":[],\"extra\":1}", false, "CPM_FEATURE_UNSUPPORTED"},
      {"invalid-root", "{\"version\":1,\"elements\":[null]}", false, "CPM_INVALID_ROOT"},
      {"noncanonical-whitespace", "{ \"version\": 1, \"elements\": [] }\r\n", true, "CPM_NON_CANONICAL"}
    };
    for (var testCase : cases) {
      var result = validator.validate(zip((String) testCase[1]));
      assertEquals(testCase[2], result.success(), testCase[0] + " diagnostics=" + result.diagnostics().all());
      assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals(testCase[3].toString())), testCase[0].toString());
    }
  }

  private static byte[] zip(String config) throws Exception {
    var out = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("config.json"));
      zip.write(config.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }
}
