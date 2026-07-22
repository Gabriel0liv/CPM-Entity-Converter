package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelIndexConfigTest {
  @Test
  void modelIndexIsUsedByCompiler() {
    var bone =
        new BoneIR(
            new BoneId("head-id"),
            "head",
            null,
            List.of(),
            Transform.identity(),
            List.of(),
            SourceLocation.of(new SourcePath("fixture")));
    var model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "geometry"),
            new GeometryId("g"),
            List.of(bone),
            List.of(bone.id()),
            List.of(
                new AnimationClipIR(
                    new ClipId("idle"),
                    1.0,
                    PlaybackMode.LOOP,
                    null,
                    List.of(),
                    List.of(),
                    SourceLocation.of(new SourcePath("fixture.animation.json")))),
            List.of(),
            List.of());
    var document =
        new MappingDocumentV1(
            1,
            Map.of("head", "head"),
            Map.of("idle", "idle"),
            null,
            new MappingDocumentV1.Sampling(20),
            List.of());
    var result = new MappingCompiler().compile(document, new ModelIndex(model));
    assertTrue(result.success());
    assertEquals(new BoneId("head-id"), result.value().bones().get("head"));
    assertEquals(new ClipId("idle"), result.value().clips().get("idle"));
  }
}
