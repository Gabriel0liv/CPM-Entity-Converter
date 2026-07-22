package io.github.gabriel0liv.cpmconverter.ir;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Relational validator matrix for graph, identity and timeline invariants. */
final class ModelIrValidatorTest {
  private static BoneIR bone(String id, BoneId parent, List<BoneId> children) {
    return new BoneIR(
        new BoneId(id), id, parent, children, Transform.identity(), List.of(), "fixture.geo");
  }

  private static ModelIR model(
      List<BoneIR> bones, List<BoneId> roots, List<AnimationClipIR> clips) {
    return new ModelIR(
        new SourceDescriptor("fixture.geo.json", "geometry"),
        new GeometryId("fixture"),
        bones,
        roots,
        clips,
        List.of(),
        List.of());
  }

  private static AnimationClipIR clip(BoneId trackBone, List<KeyframeIR<Double>> frames) {
    List<KeyframeIR<Vec3d>> vectors =
        frames.stream()
            .map(
                frame ->
                    new KeyframeIR<>(
                        frame.time(),
                        new Vec3d(frame.incomingValue(), 0, 0),
                        new Vec3d(frame.outgoingValue(), 0, 0),
                        frame.interpolation(),
                        TestSourceLocations.animation("/animations/idle/bones/body/position")))
            .toList();
    ChannelIR<Vec3d> channel =
        new ChannelIR<>("position", TransformMode.ABSOLUTE, TransformSpace.LOCAL, vectors);
    BoneTrackIR track =
        new BoneTrackIR(
            trackBone,
            channel,
            null,
            null,
            TransformMode.ABSOLUTE,
            TransformSpace.LOCAL,
            TestSourceLocations.animation("/animations/idle/bones/body"));
    return new AnimationClipIR(
        new ClipId("idle"),
        1.0,
        PlaybackMode.LOOP,
        null,
        List.of(track),
        List.of(),
        TestSourceLocations.animation("/animations/idle"));
  }

  @Test
  void acceptsMultipleRootsAndDeepHierarchy() {
    BoneIR head = bone("head", new BoneId("neck"), List.of());
    BoneIR neck = bone("neck", new BoneId("body"), List.of(new BoneId("head")));
    BoneIR body = bone("body", null, List.of(new BoneId("neck")));
    BoneIR tail = bone("tail", null, List.of());
    assertTrue(
        new ModelIrValidator()
            .validate(
                model(
                    List.of(body, neck, head, tail),
                    List.of(new BoneId("body"), new BoneId("tail")),
                    List.of()))
            .all()
            .isEmpty());
  }

  @Test
  void reportsGraphIdentityAndDanglingReferencesWithContext() {
    BoneIR root = bone("body", null, List.of(new BoneId("missing"), new BoneId("missing")));
    BoneIR duplicate = bone("body", null, List.of());
    ModelIR invalid =
        model(
            List.of(root, duplicate),
            List.of(new BoneId("body"), new BoneId("unknown")),
            List.of());
    var diagnostics = new ModelIrValidator().validate(invalid).all();
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().value().equals("IR_DUPLICATE_BONE_ID")));
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().value().equals("IR_CHILD_MISSING")));
    assertTrue(diagnostics.stream().anyMatch(d -> d.context().containsKey("childId")));
  }

  @Test
  void reportsTimelineOrderDuplicatesAndAfterDuration() {
    var frames =
        List.of(
            new KeyframeIR<>(
                0.75,
                1.0,
                1.0,
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/0.75")),
            new KeyframeIR<>(
                0.25,
                2.0,
                2.0,
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/0.25")),
            new KeyframeIR<>(
                0.25,
                3.0,
                3.0,
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/0.25b")),
            new KeyframeIR<>(
                2.0,
                4.0,
                4.0,
                InterpolationIR.LINEAR,
                TestSourceLocations.animation("/animations/idle/bones/body/position/2.0")));
    var diagnostics =
        new ModelIrValidator()
            .validate(
                model(
                    List.of(bone("body", null, List.of())),
                    List.of(new BoneId("body")),
                    List.of(clip(new BoneId("body"), frames))))
            .all();
    assertTrue(diagnostics.stream().anyMatch(d -> d.code().value().equals("IR_KEYFRAME_ORDER")));
    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().value().equals("IR_KEYFRAME_DUPLICATE")));
    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().value().equals("IR_KEYFRAME_AFTER_DURATION")));
  }
}
