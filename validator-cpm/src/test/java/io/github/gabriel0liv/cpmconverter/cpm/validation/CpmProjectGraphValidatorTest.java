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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class CpmProjectGraphValidatorTest {
  private final CpmProjectValidator validator = new CpmProjectValidator();

  @Test
  void rejectsDuplicateStoreIds() throws Exception {
    String config =
        project("body", child(1000, "0", "0", null) + "," + child(1000, "0", "0", null));

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_DUPLICATE_STORE_ID);
  }

  @Test
  void rejectsStoreIdOutsideExactJsonIntegerRange() throws Exception {
    String config = project("body", child(9007199254740992L, "0", "0", null));

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_STORE_ID_RANGE);
  }

  @Test
  void rejectsUnknownVanillaRoot() throws Exception {
    Result<CpmValidationReport> result = validate(project("wing", ""), EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_INVALID_ROOT);
  }

  @Test
  void rejectsDuplicateUnduplicatedVanillaRoot() throws Exception {
    String config =
        "{\"version\":1,\"elements\":[" + root("body", "") + "," + root("body", "") + "]}";

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_INVALID_ROOT);
  }

  @Test
  void rejectsFractionalBoxUv() throws Exception {
    String config = project("body", child(1000, "1.5", "2", null));

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_UV_INVALID);
  }

  @Test
  void rejectsFractionalPerFaceUvEndpoint() throws Exception {
    String faceUv =
        "{\"north\":{\"sx\":0,\"sy\":0,\"ex\":3.5,\"ey\":4,\"rot\":\"0\",\"autoUV\":false}}";
    String config = project("body", child(1000, "0", "0", faceUv));

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_UV_INVALID);
  }

  @Test
  void existingProfileAcceptsSafeUniqueNonSequentialStoreIds() throws Exception {
    String config =
        project("body", child(4000, "0", "0", null) + "," + child(1001, "0", "0", null));

    Result<CpmValidationReport> result = validate(config, EXISTING_V1);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  @Test
  void generatedProfileRejectsNonCanonicalPreorderStoreIds() throws Exception {
    String config =
        generatedProject(child(4000, "0", "0", null) + "," + child(1001, "0", "0", null));

    Result<CpmValidationReport> result = validate(config, GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  @Test
  void generatedProfileRejectsMissingCanonicalRoots() throws Exception {
    Result<CpmValidationReport> result = validate(project("body", ""), GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  @Test
  void generatedProfileRejectsNonCanonicalRootOrder() throws Exception {
    String config =
        "{\"version\":1,\"elements\":["
            + root("body", "")
            + ","
            + root("head", "")
            + ","
            + root("left_arm", "")
            + ","
            + root("right_arm", "")
            + ","
            + root("left_leg", "")
            + ","
            + root("right_leg", "")
            + "]}";

    Result<CpmValidationReport> result = validate(config, GENERATED_V1);

    assertFalse(result.success());
    assertHasCode(result, DiagnosticCodes.CPM_VALIDATION_FAILED);
  }

  private Result<CpmValidationReport> validate(String config, CpmValidationProfile profile)
      throws IOException {
    return validator.validate(zip(config), profile);
  }

  private static String project(String rootId, String children) {
    return "{\"version\":1,\"elements\":[" + root(rootId, children) + "]}";
  }

  private static String generatedProject(String bodyChildren) {
    return "{\"version\":1,\"elements\":["
        + root("head", "")
        + ","
        + root("body", bodyChildren)
        + ","
        + root("left_arm", "")
        + ","
        + root("right_arm", "")
        + ","
        + root("left_leg", "")
        + ","
        + root("right_leg", "")
        + "]}";
  }

  private static String root(String id, String children) {
    String childrenField = children.isEmpty() ? "" : ",\"children\":[" + children + "]";
    return "{\"id\":\""
        + id
        + "\",\"show\":false,\"pos\":{\"x\":0,\"y\":0,\"z\":0},"
        + "\"rotation\":{\"x\":0,\"y\":0,\"z\":0}"
        + childrenField
        + "}";
  }

  private static String child(long storeId, String u, String v, String faceUv) {
    String faceUvField = faceUv == null ? "" : ",\"faceUV\":" + faceUv;
    return "{\"name\":\"cube\",\"show\":true,\"texture\":false,\"textureSize\":1,"
        + "\"offset\":{\"x\":0,\"y\":0,\"z\":0},\"pos\":{\"x\":0,\"y\":0,\"z\":0},"
        + "\"rotation\":{\"x\":0,\"y\":0,\"z\":0},\"size\":{\"x\":1,\"y\":1,\"z\":1},"
        + "\"rscale\":{\"x\":1,\"y\":1,\"z\":1},\"scale\":{\"x\":1,\"y\":1,\"z\":1},"
        + "\"u\":"
        + u
        + ",\"v\":"
        + v
        + ",\"color\":\"ffffff\",\"mirror\":false,\"mcScale\":0,\"storeID\":"
        + storeId
        + faceUvField
        + "}";
  }

  private static byte[] zip(String config) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream output = new ZipOutputStream(bytes)) {
      output.putNextEntry(new ZipEntry("config.json"));
      output.write(config.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
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
