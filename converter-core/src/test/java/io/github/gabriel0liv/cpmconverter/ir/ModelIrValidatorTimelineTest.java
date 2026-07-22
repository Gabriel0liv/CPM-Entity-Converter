package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModelIrValidatorTimelineTest {
  @Test
  void reportsInvalidTimelineMetadataWithSpecificSuggestions() {
    var frames =
        List.of(
            new KeyframeIR<>(
                0.8,
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0),
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/0.8")),
            new KeyframeIR<>(
                0.2,
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0),
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/0.2")),
            new KeyframeIR<>(
                1.2,
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0),
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/1.2")));
    var track =
        new BoneTrackIR(
            new BoneId("body"),
            new ChannelIR<>("position", TransformMode.ABSOLUTE, TransformSpace.LOCAL, frames),
            null,
            null,
            TransformMode.ABSOLUTE,
            TransformSpace.LOCAL,
            TestSourceLocations.animation("/animations/idle/bones/body"));
    var clip =
        new AnimationClipIR(
            new ClipId("idle"),
            1.0,
            PlaybackMode.LOOP,
            null,
            List.of(track),
            List.of(),
            TestSourceLocations.animation("/animations/idle"));
    var model =
        new ModelIR(
            new SourceDescriptor("fixture.geo.json", "geometry"),
            new GeometryId("fixture"),
            List.of(
                new BoneIR(
                    new BoneId("body"),
                    "body",
                    null,
                    List.of(),
                    Transform.identity(),
                    List.of(),
                    "fixture.geo.json")),
            List.of(new BoneId("body")),
            List.of(clip),
            List.of(),
            List.of());
    var diagnostics = new ModelIrValidator().validate(model).all();
    assertTrue(
        diagnostics.stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_KEYFRAME_ORDER)));
    assertTrue(
        diagnostics.stream()
            .anyMatch(d -> d.code().value().equals(DiagnosticCodes.IR_KEYFRAME_AFTER_DURATION)));
    assertTrue(
        diagnostics.stream().allMatch(d -> d.suggestion() != null && !d.suggestion().isBlank()));
  }
}
