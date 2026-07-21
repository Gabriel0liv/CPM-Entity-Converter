package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingCompilerTest {
  private static ModelIndex index() {
    var head =
        new BoneIR(
            new BoneId("head-id"),
            "head",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    var neck =
        new BoneIR(
            new BoneId("neck-id"),
            "neck",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            "fixture");
    var clip = new AnimationClipIR(new ClipId("walk-id"), 1.0, PlaybackMode.LOOP, null, List.of());
    return new ModelIndex(
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "geometry"),
            new GeometryId("g"),
            List.of(head, neck),
            List.of(head.id(), neck.id()),
            List.of(clip),
            List.of(),
            List.of()));
  }

  @Test
  void compilesRootLookStateAndSamplingToIds() {
    var look =
        new MappingDocumentV1.Look("head", "neck", "inherited_split", 0.35, 0.65, false, Map.of());
    var states =
        Map.of("walking", new MappingDocumentV1.StateMapping("walk-id", "additive", false, 30));
    var doc =
        new MappingDocumentV1(
            1,
            1.0,
            0.0,
            null,
            "single_anchor",
            Map.of("body", "head"),
            Map.of(),
            Map.of("walk", "walk-id"),
            look,
            states,
            new MappingDocumentV1.Sampling(24),
            List.of("feature"),
            new MappingDocumentV1.DiagnosticPolicy(false, false));
    var result = new MappingCompiler().compile(doc, index());
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(new BoneId("head-id"), result.value().rootRoles().roles().get("body"));
    assertEquals(new BoneId("head-id"), result.value().look().head().orElseThrow());
    assertEquals(new BoneId("neck-id"), result.value().look().neck().orElseThrow());
    assertEquals(new ClipId("walk-id"), result.value().stateMappings().get("walking").clip());
    assertEquals(24, result.value().sampling().requestedFps());
    assertTrue(result.value().ignore().stream().anyMatch(rule -> rule.feature().equals("feature")));
  }

  @Test
  void optionalMissingClipIsInfoAndRequiredMissingIsError() {
    var optional = new MappingDocumentV1.StateMapping("missing", "additive", true, null);
    var doc =
        new MappingDocumentV1(
            1,
            null,
            null,
            null,
            null,
            Map.of(),
            Map.of(),
            Map.of(),
            null,
            Map.of("optional", optional),
            null,
            List.of(),
            null);
    var optionalResult = new MappingCompiler().compile(doc, index());
    assertTrue(optionalResult.success());
    assertEquals(
        "ANIM_OPTIONAL_CLIP_MISSING", optionalResult.diagnostics().all().get(0).code().value());

    var required = new MappingDocumentV1.StateMapping("missing", "additive", false, null);
    var requiredDoc =
        new MappingDocumentV1(
            1,
            null,
            null,
            null,
            null,
            Map.of(),
            Map.of(),
            Map.of(),
            null,
            Map.of("required", required),
            null,
            List.of(),
            null);
    assertFalse(new MappingCompiler().compile(requiredDoc, index()).success());
  }

  @Test
  void ambiguousBoneFailsWithCandidates() {
    var one =
        new BoneIR(
            new BoneId("a"), "same", null, List.of(), Transform.identity(), List.of(), "fixture");
    var two =
        new BoneIR(
            new BoneId("b"), "same", null, List.of(), Transform.identity(), List.of(), "fixture");
    var model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "geometry"),
            new GeometryId("g"),
            List.of(one, two),
            List.of(one.id(), two.id()),
            List.of(),
            List.of(),
            List.of());
    var doc = new MappingDocumentV1(1, Map.of("joint", "same"), Map.of(), null, null, List.of());
    var result = new MappingCompiler().compile(doc, new ModelIndex(model));
    assertFalse(result.success());
    assertTrue(
        result.diagnostics().all().stream().anyMatch(d -> d.context().containsKey("candidate.0")));
  }
}
