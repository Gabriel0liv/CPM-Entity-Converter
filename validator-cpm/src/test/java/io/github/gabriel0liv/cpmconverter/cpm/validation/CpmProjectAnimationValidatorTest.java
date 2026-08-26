package io.github.gabriel0liv.cpmconverter.cpm.validation;

import static io.github.gabriel0liv.cpmconverter.cpm.validation.CpmValidationProfile.EXISTING_V1;
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

class CpmProjectAnimationValidatorTest {
  private static final LocalDateTime FIXED_ZIP_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);
  private final CpmProjectValidator validator = new CpmProjectValidator();

  @Test
  void rejectsDanglingAnimationStoreId() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation(9999, true, "linear_loop")), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_DANGLING_ANIMATION_REF);
  }

  @Test
  void acceptsReservedVanillaAnimationReference() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation(3, true, "linear_loop")), EXISTING_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  @Test
  void rejectsRecognizedAnimationWithoutFrames() throws Exception {
    String animation =
        "{\"additive\":true,\"duration\":1000,\"interpolator\":\"linear_loop\","
            + "\"loop\":true,\"name\":\"gesture\",\"priority\":0}";

    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_FRAME_INVALID);
  }

  @Test
  void rejectsMalformedFrameComponentTransform() throws Exception {
    String animation =
        animation(1000, true, "linear_loop")
            .replace(
                "\"pos\":{\"x\":0,\"y\":0,\"z\":0}", "\"pos\":{\"x\":\"bad\",\"y\":0,\"z\":0}");

    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_FRAME_INVALID);
  }

  @Test
  void rejectsNonPositiveAnimationDuration() throws Exception {
    String animation =
        animation(1000, true, "linear_loop").replace("\"duration\":1000", "\"duration\":0");

    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_FRAME_INVALID);
  }

  @Test
  void existingProfileParsesIgnoredAnimationBeforeFilenameDispatch() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(existingArchive("ignored.json", "{not-json"), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_FRAME_INVALID);
  }

  @Test
  void existingProfileAllowsWellFormedIgnoredAnimationFilename() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(existingArchive("ignored.json", "{}"), EXISTING_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  @Test
  void existingProfileAllowsLoopInterpolatorCombinationLoadedIndependentlyByCpm() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(existingArchive(animation(1000, false, "linear_loop")), EXISTING_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  @Test
  void generatedProfileRejectsLoopInterpolatorMismatch() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(
            generatedAnimationArchive(animation(3, false, "linear_loop")), GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_FRAME_INVALID);
  }

  @Test
  void generatedProfileAcceptsCanonicalAnimation() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(
            generatedAnimationArchive(animation(3, true, "linear_loop")), GENERATED_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  @Test
  void generatedProfileRejectsDeflatedArchive() throws Exception {
    LinkedHashMap<String, byte[]> entries = generatedEntries();
    byte[] archive = zip(entries, false, true);

    Result<CpmValidationReport> result = validator.validate(archive, GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  @Test
  void generatedProfileRejectsNonCanonicalJsonEvenInStoredArchive() throws Exception {
    LinkedHashMap<String, byte[]> entries = generatedEntries();
    entries.put("config.json", nonCanonicalGeneratedConfig());
    byte[] archive = zip(entries, true, true);

    Result<CpmValidationReport> result = validator.validate(archive, GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  @Test
  void generatedProfileRejectsNonLexicalZipEntryOrder() throws Exception {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("skin.png", new byte[] {1, 2, 3});
    entries.put("config.json", canonicalGeneratedConfig());
    byte[] archive = zip(entries, true, true);

    Result<CpmValidationReport> result = validator.validate(archive, GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  @Test
  void generatedProfileAcceptsCanonicalStoredArchive() throws Exception {
    Result<CpmValidationReport> result =
        validator.validate(zip(generatedEntries(), true, true), GENERATED_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  private static byte[] existingArchive(String animation) throws IOException {
    return existingArchive("g_gesture.json", animation);
  }

  private static byte[] existingArchive(String fileName, String animation) throws IOException {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("config.json", existingConfig().getBytes(StandardCharsets.UTF_8));
    entries.put("animations/" + fileName, animation.getBytes(StandardCharsets.UTF_8));
    return zip(entries, false, false);
  }

  private static byte[] generatedAnimationArchive(String animation) throws IOException {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("animations/g_gesture.json", (animation + "\n").getBytes(StandardCharsets.UTF_8));
    entries.put("config.json", canonicalGeneratedConfig());
    entries.put("skin.png", new byte[] {1, 2, 3});
    return zip(entries, true, true);
  }

  private static String existingConfig() {
    return "{\"version\":1,\"elements\":[{\"id\":\"body\",\"show\":false,"
        + "\"children\":[{\"name\":\"cube\",\"storeID\":1000,\"u\":0,\"v\":0}]}]}";
  }

  private static String animation(long storeId, boolean loop, String interpolator) {
    return "{\"additive\":true,\"duration\":1000,\"frames\":[{\"components\":[{"
        + "\"color\":\"ffffff\",\"pos\":{\"x\":0,\"y\":0,\"z\":0},"
        + "\"rotation\":{\"x\":0,\"y\":0,\"z\":0},"
        + "\"scale\":{\"x\":1,\"y\":1,\"z\":1},\"show\":true,\"storeID\":"
        + storeId
        + "}]}],\"interpolator\":\""
        + interpolator
        + "\",\"loop\":"
        + loop
        + ",\"name\":\"gesture\",\"priority\":0}";
  }

  private static LinkedHashMap<String, byte[]> generatedEntries() {
    LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("config.json", canonicalGeneratedConfig());
    entries.put("skin.png", new byte[] {1, 2, 3});
    return entries;
  }

  private static byte[] canonicalGeneratedConfig() {
    return ("{\"elements\":[{\"id\":\"head\"},{\"id\":\"body\"},{\"id\":\"left_arm\"},"
            + "{\"id\":\"right_arm\"},{\"id\":\"left_leg\"},{\"id\":\"right_leg\"}],\"version\":1}\n")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] nonCanonicalGeneratedConfig() {
    return ("{\"version\":1,\"elements\":[{\"id\":\"head\"},{\"id\":\"body\"},"
            + "{\"id\":\"left_arm\"},{\"id\":\"right_arm\"},{\"id\":\"left_leg\"},"
            + "{\"id\":\"right_leg\"}]}")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] zip(
      LinkedHashMap<String, byte[]> entries, boolean stored, boolean fixedTimestamp)
      throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream output = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> source : entries.entrySet()) {
        byte[] payload = source.getValue();
        ZipEntry entry = new ZipEntry(source.getKey());
        if (fixedTimestamp) entry.setTimeLocal(FIXED_ZIP_TIME);
        if (stored) {
          CRC32 crc = new CRC32();
          crc.update(payload);
          entry.setMethod(ZipEntry.STORED);
          entry.setSize(payload.length);
          entry.setCompressedSize(payload.length);
          entry.setCrc(crc.getValue());
        }
        output.putNextEntry(entry);
        output.write(payload);
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
