package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIndex;
import io.github.gabriel0liv.cpmconverter.ir.SourceDescriptor;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;

class ConfigTest {
  @Test
  void jsonYamlParity() throws Exception {
    Path j = Files.createTempFile("m", ".json"), y = Files.createTempFile("m", ".yaml");
    String s =
        "{\"schemaVersion\":1,\"bones\":{\"head\":\"head\"},\"clips\":{\"idle\":\"idle\"},\"sampling\":{\"requestedFps\":20}}";
    Files.writeString(j, s);
    Files.writeString(
        y,
        "schemaVersion: 1\nbones: {head: head}\nclips: {idle: idle}\nsampling: {requestedFps: 20}\n");
    var l = new MappingLoader();
    assertEquals(l.load(j).value().schemaVersion(), l.load(y).value().schemaVersion());
    assertFalse(new MappingValidator().validate(l.load(j).value()).hasErrors());
  }

  @Test
  void range() {
    assertTrue(
        new MappingValidator()
            .validate(
                new MappingDocumentV1(
                    1,
                    java.util.Map.of(),
                    java.util.Map.of(),
                    null,
                    new MappingDocumentV1.Sampling(0),
                    java.util.List.of()))
            .hasErrors());
  }

  @Test
  void compilerPreservesLookLimits() {
    MappingDocumentV1 document = documentWithLookLimits(Map.of("yaw", 70d, "pitch", 45d));

    var result = new MappingCompiler().compile(document, new ModelIndex(modelWithHead()));

    assertTrue(result.success());
    assertEquals(Map.of("yaw", 70d, "pitch", 45d), result.value().look().limits());
    assertThrows(
        UnsupportedOperationException.class, () -> result.value().look().limits().put("roll", 10d));
  }

  @Test
  void semanticValidatorRejectsInvalidLookLimits() {
    MappingDocumentV1 negative = documentWithLookLimits(Map.of("yaw", -1d));
    MappingDocumentV1 nonFinite = documentWithLookLimits(Map.of("yaw", Double.NaN));

    assertTrue(
        new MappingValidator()
            .validate(negative).errors().stream()
                .anyMatch(d -> d.code().value().equals(DiagnosticCodes.CONFIG_LOOK_LIMIT)));
    assertTrue(
        new MappingValidator()
            .validate(nonFinite).errors().stream()
                .anyMatch(d -> d.code().value().equals(DiagnosticCodes.CONFIG_LOOK_LIMIT)));
  }

  @Test
  void compilerRunsSemanticValidationBeforeResolution() {
    MappingDocumentV1 invalid = documentWithLookLimits(Map.of("yaw", -1d));

    var result = new MappingCompiler().compile(invalid, new ModelIndex(modelWithHead()));

    assertFalse(result.success());
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.CONFIG_LOOK_LIMIT)));
  }

  private static MappingDocumentV1 documentWithLookLimits(Map<String, Double> limits) {
    return new MappingDocumentV1(
        1,
        null,
        null,
        null,
        "single_anchor",
        Map.of(),
        Map.of(),
        Map.of(),
        new MappingDocumentV1.Look("head", null, "independent", 0d, 1d, false, limits),
        Map.of(),
        new MappingDocumentV1.Sampling(20),
        List.of(),
        null);
  }

  private static ModelIR modelWithHead() {
    BoneIR head =
        new BoneIR(
            new BoneId("head"),
            "head",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    return new ModelIR(
        new SourceDescriptor("fixture.geo.json", "1.12.0"),
        List.of(head),
        List.of(head.id()),
        List.of(),
        List.of());
  }
}
