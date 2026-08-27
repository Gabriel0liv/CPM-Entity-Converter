package io.github.gabriel0liv.cpmconverter.cpm.validation;

import static io.github.gabriel0liv.cpmconverter.cpm.validation.CpmValidationProfile.EXISTING_V1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class CpmProjectValidatorTest {
  private final CpmProjectValidator validator = new CpmProjectValidator();

  @Test
  void acceptsLoaderCompatibleV1Container() throws Exception {
    byte[] archive =
        zip(
            Map.of(
                "config.json",
                json(
                    "{\"version\":1,\"elements\":[{\"id\":\"body\",\"pos\":{\"x\":0,\"y\":0,\"z\":0},\"rotation\":{\"x\":0,\"y\":0,\"z\":0},\"show\":false}]}")));

    Result<CpmValidationReport> result = validator.validate(archive, EXISTING_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(1, result.value().entryCount());
    assertEquals(1, result.value().elementCount());
    assertEquals(0, result.value().animationCount());
    assertEquals(0, result.value().storeIdCount());
  }

  @Test
  void rejectsNonZipPayload() {
    Result<CpmValidationReport> result =
        validator.validate("not a zip".getBytes(StandardCharsets.UTF_8), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_ZIP_INVALID);
  }

  @Test
  void rejectsArchiveWithoutConfig() throws Exception {
    Result<CpmValidationReport> result = validator.validate(zip(Map.of()), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_CONFIG_INVALID);
  }

  @Test
  void rejectsConfigWithoutElements() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(zip(Map.of("config.json", json("{\"version\":1}"))), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_CONFIG_INVALID);
  }

  @Test
  void rejectsUnsupportedProjectVersion() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(
            zip(Map.of("config.json", json("{\"version\":2,\"elements\":[]}"))), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);
  }

  private static byte[] json(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] zip(Map<String, byte[]> entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream output = new ZipOutputStream(bytes)) {
      LinkedHashMap<String, byte[]> ordered = new LinkedHashMap<>();
      entries.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
      for (Map.Entry<String, byte[]> entry : ordered.entrySet()) {
        output.putNextEntry(new ZipEntry(entry.getKey()));
        output.write(entry.getValue());
        output.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  private static void assertHasCode(Result<?> result, String code) {
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> result.diagnostics().all().toString());
  }
}
