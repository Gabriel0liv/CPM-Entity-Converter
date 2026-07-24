package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import org.junit.jupiter.api.Test;

class CpmPersistedProjectParserTest {
  @Test
  void malformedRootProducesFailureInsteadOfThrowing() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[null]}");
    var config = new CpmValidatedConfigV1(json, 1, "default", new CpmPersistedSize2i(64, 64), false, false);
    var result = assertDoesNotThrow(() -> new CpmPersistedProjectParser().parse(json, config, null, false, CpmArtifactLimits.defaults()));
    assertFalse(result.success());
  }

  @Test
  void accumulatesIndependentSchemaDiagnosticsWithPointers() throws Exception {
    var json = new ObjectMapper().readTree(
        "{\"version\":1,\"elements\":[{\"id\":\"body\",\"show\":\"yes\",\"unknown\":true,\"pos\":{\"x\":\"bad\",\"y\":0,\"z\":0},\"children\":7}]}" );
    var result = parse(json, CpmArtifactLimits.defaults());
    assertFalse(result.success());
    var pointers = result.diagnostics().all().stream().map(d -> d.location().jsonPointer()).collect(java.util.stream.Collectors.toSet());
    assertTrue(pointers.contains("/elements/0/unknown"));
    assertTrue(pointers.contains("/elements/0/show"));
    assertTrue(pointers.contains("/elements/0/pos/x"));
    assertTrue(pointers.contains("/elements/0/children"));
  }

  @Test
  void materializesElementsInPreOrder() throws Exception {
    var json = new ObjectMapper().readTree(
        "{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"name\":\"A\",\"storeID\":1000,\"children\":[{\"name\":\"A1\",\"storeID\":1001},{\"name\":\"A2\",\"storeID\":1002}]},{\"name\":\"B\",\"storeID\":1003,\"children\":[{\"name\":\"B1\",\"storeID\":1004}]}]}]}" );
    var result = parse(json, CpmArtifactLimits.defaults());
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(java.util.List.of("A", "A1", "A2", "B", "B1"), result.value().elements().stream().map(CpmPersistedElementV1::name).toList());
    assertEquals(java.util.List.of(0, 1, 2, 3, 4), result.value().elements().stream().map(CpmPersistedElementV1::preOrderIndex).toList());
  }

  @Test
  void elementLimitProducesExplicitDiagnostic() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"storeID\":1000},{\"storeID\":1001}]}]}");
    var result = parse(json, limits(1, 256));
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("INPUT_LIMIT_EXCEEDED") && "/elements/0/children/1".equals(d.location().jsonPointer())));
  }

  @Test
  void strictUvRejectsUnknownFaceAndInvalidRotation() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"storeID\":1000,\"faceUV\":{\"north\":{\"sx\":0,\"sy\":0,\"ex\":1,\"ey\":1,\"rot\":\"45\"},\"diagonal\":{}}}]}]}");
    var result = parse(json, CpmArtifactLimits.defaults());
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("UV_INVALID") && d.location().jsonPointer().endsWith("/north/rot")));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("UV_FACE_UNKNOWN")));
  }

  @Test
  void duplicateAndCustomRootsPreservePersistedAndEffectiveIds() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"body\"},{\"id\":\"body\",\"dup\":true,\"storeID\":1001},{\"id\":\"hat\",\"customPart\":true,\"storeID\":1002}]}");
    var result = parse(json, CpmArtifactLimits.defaults());
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(3, result.value().roots().size());
    assertEquals(CpmPersistedRootKind.VANILLA, result.value().roots().get(0).kind());
    assertEquals(CpmPersistedRootKind.DUPLICATE, result.value().roots().get(1).kind());
    assertEquals(CpmPersistedRootKind.CUSTOM, result.value().roots().get(2).kind());
    assertEquals(1001L, result.value().roots().get(1).persistedStoreId());
    assertEquals(1002L, result.value().roots().get(2).effectiveStoreId());
    assertTrue(result.value().effectiveTargets().containsKey(1001L));
    assertTrue(result.value().effectiveTargets().containsKey(1002L));
  }

  @Test
  void depthLimitProducesDiagnosticAtExceededChild() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"body\",\"children\":[{\"storeID\":1000,\"children\":[{\"storeID\":1001,\"children\":[{\"storeID\":1002}]}]}]}]}");
    var result = parse(json, limits(10, 1));
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("INPUT_LIMIT_EXCEEDED") && "/elements/0/children/0/children/0/children/0".equals(d.location().jsonPointer())));
  }

  @Test
  void reservedRootStoreIdIsRejectedWithoutThrowing() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"body\",\"dup\":true,\"storeID\":6}]}");
    var result = assertDoesNotThrow(() -> parse(json, CpmArtifactLimits.defaults()));
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> d.code().value().equals("CPM_INVALID_STORE_ID") && "/elements/0/storeID".equals(d.location().jsonPointer())));
  }

  @Test
  void customAndDuplicateFlagsAreRejectedTogether() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[{\"id\":\"hat\",\"customPart\":true,\"dup\":true,\"storeID\":1001}]}");
    var result = assertDoesNotThrow(() -> parse(json, CpmArtifactLimits.defaults()));
    assertFalse(result.success());
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> "/elements/0/customPart".equals(d.location().jsonPointer())));
    assertTrue(result.diagnostics().all().stream().anyMatch(d -> "/elements/0/dup".equals(d.location().jsonPointer())));
  }

  private static Result<CpmPersistedProjectV1> parse(com.fasterxml.jackson.databind.JsonNode json, CpmArtifactLimits limits) {
    var config = new CpmValidatedConfigV1(json, 1, "default", new CpmPersistedSize2i(64, 64), false, false);
    return new CpmPersistedProjectParser().parse(json, config, null, false, limits);
  }

  private static CpmArtifactLimits limits(int maxElements, int maxDepth) {
    var d = CpmArtifactLimits.defaults();
    return new CpmArtifactLimits(d.maxArtifactBytes(), d.maxEntries(), d.maxEntryNameLength(), d.maxEntryUncompressedBytes(), d.maxTotalUncompressedBytes(), d.maxCompressionRatio(), d.maxConfigBytes(), d.maxAnimationJsonBytes(), d.maxSkinBytes(), d.maxJsonDepth(), d.maxStringLength(), d.maxNumberLength(), maxElements, maxDepth, d.maxAnimations(), d.maxFramesPerAnimation(), d.maxComponentsPerFrame(), d.maxPngChunks());
  }
}
