package io.github.gabriel0liv.cpmconverter.cpm.validation;

import static io.github.gabriel0liv.cpmconverter.cpm.validation.CpmValidationProfile.GENERATED_V1;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class CpmProjectDeterministicArchiveValidatorTest {
  private static final LocalDateTime NONCANONICAL_ZIP_TIME = LocalDateTime.of(1980, 1, 2, 0, 0);
  private final CpmProjectValidator validator = new CpmProjectValidator();

  @Test
  void generatedProfileRejectsNonFixedZipTimestamp() throws Exception {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("config.json", canonicalGeneratedConfig());
    entries.put("skin.png", new byte[] {1, 2, 3});

    Result<CpmValidationReport> result =
        validator.validate(storedZip(entries, NONCANONICAL_ZIP_TIME), GENERATED_V1);

    assertFalse(result.success());
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.CPM_VALIDATION_FAILED)),
        () -> result.diagnostics().all().toString());
  }

  private static byte[] canonicalGeneratedConfig() {
    return ("{\"elements\":[{\"id\":\"head\"},{\"id\":\"body\"},{\"id\":\"left_arm\"},"
            + "{\"id\":\"right_arm\"},{\"id\":\"left_leg\"},{\"id\":\"right_leg\"}],\"version\":1}\n")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] storedZip(LinkedHashMap<String, byte[]> entries, LocalDateTime timestamp)
      throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream output = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> source : entries.entrySet()) {
        byte[] payload = source.getValue();
        CRC32 crc = new CRC32();
        crc.update(payload);

        ZipEntry entry = new ZipEntry(source.getKey());
        entry.setTimeLocal(timestamp);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(payload.length);
        entry.setCompressedSize(payload.length);
        entry.setCrc(crc.getValue());

        output.putNextEntry(entry);
        output.write(payload);
        output.closeEntry();
      }
    }
    return bytes.toByteArray();
  }
}
