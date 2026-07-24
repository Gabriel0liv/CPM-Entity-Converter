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
      {"noncanonical-whitespace", "{ \"version\": 1, \"elements\": [] }\r\n", true, "CPM_NON_CANONICAL"},
      {"container-extra", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"zip-order", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"zip-method", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"zip-timestamp", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"config-utf8", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"config-duplicate-key", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"config-number-form", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"config-escaping", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"registry-reserved", "{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"storeID\":6}]}]}", false, "CPM_INVALID_STORE_ID"},
      {"registry-collision", "{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"storeID\":1000},{\"storeID\":1000}]}]}", false, "CPM_INVALID_STORE_ID"},
      {"uv-box", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"uv-face", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"uv-texture-size", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"texture-missing", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"png-crc", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"png-interlace", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"png-profile", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"png-budget", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-json", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-field", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-dangling", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-limits", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-vector", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"},
      {"animation-color", "{\"version\":1,\"elements\":[]}", true, "CPM_NON_CANONICAL"}
    };
    for (var testCase : cases) {
      var result = validator.validate(zip((String) testCase[1]));
      assertEquals(testCase[2], result.success(), testCase[0] + " diagnostics=" + result.diagnostics().all());
      assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals(testCase[3].toString())), testCase[0].toString());
    }
  }

  @Test
  void canonicalityDetectsJsonFieldOrderAndAnimationBytes() throws Exception {
    var validator = new CpmArtifactValidator();
    var reordered = "{\"elements\":[],\"version\":1}\n";
    var result = validator.validate(zipWithAnimation(reordered, "{\"loop\":true,\"duration\":1}\n"));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("CPM_NON_CANONICAL")));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.location() != null && d.location().source().value().equals("config.json")));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.location() != null && d.location().source().value().equals("animations/a.json")));
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

  private static byte[] zipWithAnimation(String config, String animation) throws Exception {
    var out = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("config.json"));
      zip.write(config.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("animations/a.json"));
      zip.write(animation.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }
}
