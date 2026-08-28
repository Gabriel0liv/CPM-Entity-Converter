# T400 Animation Sampler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a deterministic production sampler that evaluates GeckoLib 4.4.9 animation channels onto the ADR-004 uniform time grid and emits sampled per-bone animation deltas for T401/T402.

**Architecture:** `converter-core` owns sampled IR, timeline/grid generation, channel segment selection, identity completion, rotation continuity, diagnostics, and the `AnimationSampler`. Gecko-specific easing remains behind a tiny `InterpolationEvaluator` port implemented by `adapter-geckolib4`. `converter-config` only resolves requested FPS precedence; T400 does not compose bind/hierarchy transforms and does not write CPM animations.

**Tech Stack:** Java 17, Gradle Wrapper, JUnit 5, existing `converter-core` math/IR/diagnostics, existing GeckoLib 4.4.9 adapter/easing evaluator, GitHub Actions Ubuntu/Windows matrix.

**Spec:** `docs/superpowers/specs/2026-08-28-t400-animation-sampler-design.md`

## Global Constraints

- Java 17 only.
- Default sampling rate is exactly 20 FPS; valid configured range is `1..240`.
- FPS precedence is `stateMapping.requestedFps -> sampling.requestedFps -> 20`.
- `converter-core` gains no Minecraft, Forge, GeckoLib, CPM, Blockbench, Jackson, or other runtime dependency.
- Identical logical input/configuration must produce deterministic logical samples on Windows and Linux.
- Frame times are computed directly from `D`, `N`, and frame index; never by accumulating `time += interval`.
- All model bones appear in every sampled frame in canonical model order; missing tracks/channels use identity values.
- T400 samples animation transforms only; no `bindLocal`, hierarchy/world composition, model scale, vertical offset, look/retarget, CPM projection, loop-seam policy, or hold/play-once terminal policy.
- Source rotations remain continuous Euler values through temporal interpolation; no keyframe SLERP and no normalization to `[-180,180]` before continuity metadata is captured.
- `PlaybackMode.LOOP -> TimelineKind.LOOP`; `PLAY_ONCE` and `HOLD -> TimelineKind.SINGLE`; unclassified `CUSTOM` fails explicitly.
- TDD is mandatory: every production behavior is introduced by a failing test, observed RED, then minimal GREEN.
- Do not mark T400 `[x]` until the repository gate and Ubuntu/Windows CI are green.

---

## File Structure

Production files created in `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/`:

- `TimelineKind.java` — LOOP/SINGLE grid distinction only.
- `SamplingRequest.java` — `ClipId`, requested FPS, and timeline kind; validates the direct sampler request.
- `SampledClipKey.java` — cache/result identity `(clipId, requestedFps, timelineKind)`.
- `SamplingMetadataIR.java` — ADR-004 metrics.
- `SampledBoneTransformIR.java` — one bone's sampled transform plus optional source track semantics.
- `SampledFrameIR.java` — frame index/time and ordered bones.
- `SampledClipIR.java` — complete sampled clip result.
- `InterpolationEvaluator.java` — scalar progress evaluator port.
- `TimelineGrid.java` — pure ADR-004 frame count/timestamps/metadata calculation.
- `VectorChannelSampler.java` — ordinary `ChannelIR<Vec3d>` segment selection/evaluation.
- `RotationChannelSampler.java` — continuous source Euler evaluation and ZYX quaternion conversion.
- `AnimationSampler.java` — orchestration across all canonical model bones.

Existing production files modified:

- `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/diagnostics/DiagnosticCodes.java` — narrow stable T400 diagnostic codes.
- `converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolver.java` — new precedence helper using existing compiled config records.
- `adapter-geckolib4/src/main/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluator.java` — implements core port by delegating to existing `Gecko449EasingEvaluator`.

Tests created:

- `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/TimelineGridTest.java`
- `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/VectorChannelSamplerTest.java`
- `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/RotationChannelSamplerTest.java`
- `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSamplerTest.java`
- `converter-config/src/test/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolverTest.java`
- `adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluatorTest.java`
- `adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/T400GeckoOracleSamplingTest.java`

Final documentation modified only after GREEN:

- `specs/001-geckolib4-to-cpm/tasks.md`
- `specs/001-geckolib4-to-cpm/test-plan.md`
- `specs/001-geckolib4-to-cpm/traceability.md`

---

### Task 1: Lock the sampled IR and ADR-004 timeline grid

**Files:**
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/TimelineKind.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SamplingRequest.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SampledClipKey.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SamplingMetadataIR.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SampledBoneTransformIR.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SampledFrameIR.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/SampledClipIR.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/TimelineGrid.java`
- Test: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/TimelineGridTest.java`

**Interfaces:**
- Consumes: existing `ClipId`, `PlaybackMode`, `BoneId`, `SampledTransformIR`, `TransformMode`, `TransformSpace`.
- Produces:
  - `enum TimelineKind { LOOP, SINGLE }`
  - `record SamplingRequest(ClipId clipId, int requestedFps, TimelineKind timelineKind)`
  - `record SampledClipKey(ClipId clipId, int requestedFps, TimelineKind timelineKind)`
  - `record SamplingMetadataIR(int requestedFps, int frameCount, double frameDensity, double effectiveIntervalRate, double frameInterval, double maxTemporalGridError)`
  - `record TrackSemanticsIR(TransformMode mode, TransformSpace space)` nested in or adjacent to `SampledBoneTransformIR`; unanimated bones use `Optional.empty()`.
  - `record SampledBoneTransformIR(BoneId bone, SampledTransformIR transform, Optional<TrackSemanticsIR> trackSemantics)`
  - `record SampledFrameIR(int index, double timeSeconds, List<SampledBoneTransformIR> bones)`
  - `record SampledClipIR(SampledClipKey key, double sourceDurationSeconds, PlaybackMode sourcePlayback, SamplingMetadataIR metadata, List<SampledFrameIR> frames)`
  - `TimelineGrid.build(double durationSeconds, int requestedFps, TimelineKind kind) -> TimelineGrid.Result` where result exposes immutable `List<Double> times()` and `SamplingMetadataIR metadata()`.

- [ ] **Step 1: Write failing timeline/IR contract tests**

Create `TimelineGridTest` with explicit examples instead of formula-only assertions:

```java
@Test
void loopAt20FpsDoesNotDuplicateDurationEndpoint() {
  var grid = TimelineGrid.build(1.0, 20, TimelineKind.LOOP);

  assertEquals(20, grid.metadata().frameCount());
  assertEquals(0.0, grid.times().get(0), 0.0);
  assertEquals(0.95, grid.times().get(19), 1e-12);
  assertFalse(grid.times().contains(1.0));
  assertEquals(0.05, grid.metadata().frameInterval(), 1e-12);
  assertEquals(20.0, grid.metadata().frameDensity(), 1e-12);
  assertEquals(20.0, grid.metadata().effectiveIntervalRate(), 1e-12);
}

@Test
void singleAt20FpsIncludesExactDurationEndpoint() {
  var grid = TimelineGrid.build(1.0, 20, TimelineKind.SINGLE);

  assertEquals(20, grid.metadata().frameCount());
  assertEquals(0.0, grid.times().get(0), 0.0);
  assertEquals(1.0, grid.times().get(19), 0.0);
  assertEquals(1.0 / 19.0, grid.metadata().frameInterval(), 1e-12);
  assertEquals(19.0, grid.metadata().effectiveIntervalRate(), 1e-12);
  assertEquals(20.0, grid.metadata().frameDensity(), 1e-12);
}

@Test
void nOneSingleUsesZeroIntervalAndRate() {
  var grid = TimelineGrid.build(0.01, 20, TimelineKind.SINGLE);
  assertEquals(List.of(0.0), grid.times());
  assertEquals(1, grid.metadata().frameCount());
  assertEquals(0.0, grid.metadata().frameInterval(), 0.0);
  assertEquals(0.0, grid.metadata().effectiveIntervalRate(), 0.0);
  assertEquals(100.0, grid.metadata().frameDensity(), 1e-12);
}

@Test
void frameCountUsesJavaMathRoundAtHalfBoundary() {
  assertEquals(3, TimelineGrid.build(0.125, 20, TimelineKind.LOOP).metadata().frameCount());
}

@Test
void timestampsAreIndexDerivedAndMetadataRecordsGridError() {
  var grid = TimelineGrid.build(0.37, 20, TimelineKind.LOOP);
  for (int i = 0; i < grid.times().size(); i++) {
    assertEquals(i * 0.37 / grid.metadata().frameCount(), grid.times().get(i), 0.0);
  }
  double expected = 0;
  for (int i = 0; i < grid.times().size(); i++) {
    expected = Math.max(expected, Math.abs(grid.times().get(i) - i / 20.0));
  }
  assertEquals(expected, grid.metadata().maxTemporalGridError(), 0.0);
}
```

Add request/key immutability and `1..240` validation tests.

- [ ] **Step 2: Run the focused test and observe RED**

Run:

```bash
./gradlew :converter-core:test --tests '*TimelineGridTest'
```

Expected: compilation failure because the new sampling types do not exist.

- [ ] **Step 3: Implement the minimal sampled records and timeline grid**

Core formula must be index-derived:

```java
int frameCount = Math.max(1, (int) Math.round(durationSeconds * requestedFps));
List<Double> times = new ArrayList<>(frameCount);
if (kind == TimelineKind.LOOP) {
  for (int i = 0; i < frameCount; i++) times.add(i * durationSeconds / frameCount);
} else if (frameCount == 1) {
  times.add(0.0);
} else {
  for (int i = 0; i < frameCount; i++) times.add(i * durationSeconds / (frameCount - 1));
}
```

Calculate `maxTemporalGridError` from those exact generated timestamps. Every record constructor copies lists and rejects null/non-finite structural values.

- [ ] **Step 4: Run focused + core tests**

Run:

```bash
./gradlew :converter-core:test --tests '*TimelineGridTest'
./gradlew :converter-core:test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 1**

```bash
git add converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/TimelineGridTest.java
git commit -m "feat: add T400 sampled animation grid"
```

---

### Task 2: Resolve configured FPS precedence without coupling config to sampling internals

**Files:**
- Create: `converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolver.java`
- Create: `converter-config/src/test/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolverTest.java`

**Interfaces:**
- Consumes: `CompiledStateMapping`, `CompiledSamplingPolicy`.
- Produces: `SamplingFpsResolver.resolve(CompiledStateMapping state, CompiledSamplingPolicy global) -> int`.
- Rule: state override first; global second; exact default `20` third.

- [ ] **Step 1: Write precedence tests**

```java
@Test
void stateOverrideWinsOverGlobal() {
  var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, 40);
  assertEquals(40, SamplingFpsResolver.resolve(state, new CompiledSamplingPolicy(20)));
}

@Test
void globalWinsWhenStateHasNoOverride() {
  var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, null);
  assertEquals(30, SamplingFpsResolver.resolve(state, new CompiledSamplingPolicy(30)));
}

@Test
void defaultIsExactlyTwenty() {
  var state = new CompiledStateMapping(new ClipId("walk"), "ABSOLUTE", false, null);
  assertEquals(20, SamplingFpsResolver.resolve(state, null));
}
```

Also test `state == null` with global/default because sampling outside a state mapping may still use the global policy.

- [ ] **Step 2: Run RED**

```bash
./gradlew :converter-config:test --tests '*SamplingFpsResolverTest'
```

Expected: compilation failure because resolver does not exist.

- [ ] **Step 3: Implement the minimal resolver**

```java
public final class SamplingFpsResolver {
  private SamplingFpsResolver() {}

  public static int resolve(CompiledStateMapping state, CompiledSamplingPolicy global) {
    if (state != null && state.requestedFps() != null) return state.requestedFps();
    if (global != null) return global.requestedFps();
    return 20;
  }
}
```

Do not duplicate range validation here; compiled records already enforce it, while direct sampler request validation remains defensive in `SamplingRequest`.

- [ ] **Step 4: Run config tests**

```bash
./gradlew :converter-config:test --tests '*SamplingFpsResolverTest'
./gradlew :converter-config:test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 2**

```bash
git add converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolver.java converter-config/src/test/java/io/github/gabriel0liv/cpmconverter/config/SamplingFpsResolverTest.java
git commit -m "feat: resolve T400 sampling fps precedence"
```

---

### Task 3: Add the interpolation port and sample ordinary vector channels

**Files:**
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/InterpolationEvaluator.java`
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/VectorChannelSampler.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/diagnostics/DiagnosticCodes.java`
- Test: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/VectorChannelSamplerTest.java`

**Interfaces:**
- Produces:

```java
@FunctionalInterface
public interface InterpolationEvaluator {
  double evaluate(InterpolationIR interpolation, List<Double> easingArgs, double normalizedProgress);
}
```

- `VectorChannelSampler.sample(ChannelIR<Vec3d> channel, double timeSeconds, Vec3d identity, InterpolationEvaluator evaluator) -> Result<Vec3d>`.
- Add diagnostic codes:
  - `ANIM_SAMPLING_REQUEST_INVALID`
  - `ANIM_SAMPLING_PLAYBACK_UNSUPPORTED`
  - `ANIM_SAMPLING_CHANNEL_INVALID`
  - `ANIM_SAMPLING_NON_FINITE`

- [ ] **Step 1: Write RED tests for channel semantics**

Use direct synthetic `ChannelIR<Vec3d>` values. At minimum:

```java
private static final InterpolationEvaluator LINEAR_EVALUATOR =
    (kind, args, t) -> switch (kind) {
      case LINEAR -> t;
      case STEP -> 0.0;
      default -> t * t;
    };

@Test
void missingChannelReturnsProvidedIdentity() {
  assertEquals(Vec3d.ZERO,
      VectorChannelSampler.sample(null, 0.5, Vec3d.ZERO, LINEAR_EVALUATOR).value());
}

@Test
void linearChannelInterpolatesOutgoingToNextIncoming() {
  var channel = new ChannelIR<>("position", TransformMode.ADDITIVE, TransformSpace.LOCAL, List.of(
      new KeyframeIR<>(0.0, new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), InterpolationIR.LINEAR),
      new KeyframeIR<>(1.0, new Vec3d(10, 20, 30), new Vec3d(10, 20, 30), InterpolationIR.LINEAR)));
  assertEquals(new Vec3d(5, 10, 15),
      VectorChannelSampler.sample(channel, 0.5, Vec3d.ZERO, LINEAR_EVALUATOR).value());
}

@Test
void exactKeyframeTimeUsesThatKeyframesEffectiveValue() {
  // Assert the pinned boundary policy, not an epsilon-shifted neighbor.
}

@Test
void independentEasingProgressIsAppliedComponentwiseToVectorDifference() {
  // Evaluator returns 0.25 at t=0.5; expect 25% vector interpolation.
}
```

Add explicit before-first/after-last tests using the behavior already demonstrated by the pinned Gecko 4.4.9 oracle fixtures. If repository evidence shows clamp-to-edge for channel evaluation, encode exact edge values; if evidence differs, encode that exact evidenced behavior before production code.

- [ ] **Step 2: Run RED**

```bash
./gradlew :converter-core:test --tests '*VectorChannelSamplerTest'
```

Expected: compile failure/new behavior missing.

- [ ] **Step 3: Implement segment selection and vector interpolation**

Use binary search or a deterministic linear scan initially; do not optimize prematurely. Segment evaluation must conceptually be:

```java
KeyframeIR<Vec3d> left = keyframes.get(segmentIndex);
KeyframeIR<Vec3d> right = keyframes.get(segmentIndex + 1);
double span = right.time() - left.time();
double raw = span == 0 ? 0 : (timeSeconds - left.time()) / span;
double eased = evaluator.evaluate(left.interpolation(), left.easingArgs(), raw);
Vec3d from = left.outgoingValue();
Vec3d to = right.incomingValue();
return lerp(from, to, eased);
```

Do not clamp or normalize `eased`; Gecko bounce/back/elastic compatibility may legitimately overshoot. Reject non-finite evaluator output with `ANIM_SAMPLING_NON_FINITE`.

- [ ] **Step 4: Run focused + core tests**

```bash
./gradlew :converter-core:test --tests '*VectorChannelSamplerTest'
./gradlew :converter-core:test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 3**

```bash
git add converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/diagnostics/DiagnosticCodes.java converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/VectorChannelSamplerTest.java
git commit -m "feat: sample T400 vector animation channels"
```

---

### Task 4: Sample continuous authored Euler rotations without losing winding

**Files:**
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/RotationChannelSampler.java`
- Test: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/RotationChannelSamplerTest.java`
- Reuse: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/ir/RotationContinuityIR.java`
- Reuse: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/ir/SampledTransformIR.java`
- Reuse: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/math/Quatd.java`

**Interfaces:**
- Produces `RotationChannelSampler.sample(SourceRotationChannelIR channel, double timeSeconds, InterpolationEvaluator evaluator) -> Result<RotationSample>`.
- `RotationSample` contains `Quatd rotation()` and `RotationContinuityIR continuity()`; keep it package-local or a focused public record if `AnimationSampler` needs it directly.
- `sourceEulerHint` is the unnormalized sampled authored Euler value in degrees.
- `winding` is derived from that authored hint using an explicit tested convention: integer full turns per axis relative to the principal equivalent, preserving `360`, `720`, etc.

- [ ] **Step 1: Write RED continuity tests**

```java
@Test
void preservesAuthoredTwoTurnWinding() {
  var channel = rotations(
      key(0.0, 0),
      key(1.0, 360),
      key(2.0, 720));

  var sample = RotationChannelSampler.sample(channel, 1.5, LINEAR_EVALUATOR).value();
  assertEquals(540.0, sample.continuity().sourceEulerHint().x(), 1e-12);
  assertEquals(1, sample.continuity().winding().x());
}

@Test
void doesNotRewriteAuthored179ToMinus179AsShortestPath() {
  var channel = rotations(key(0.0, 179), key(1.0, -179));
  var mid = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR).value();
  assertEquals(0.0, mid.continuity().sourceEulerHint().x(), 1e-12);
}

@Test
void convertsInstantaneousEulerUsingZyxQuaternion() {
  var channel = rotationXYZAtHalfSample();
  var actual = RotationChannelSampler.sample(channel, 0.5, LINEAR_EVALUATOR).value().rotation();
  var expected = Quatd.fromEulerZYX(
      Math.toRadians(expectedX), Math.toRadians(expectedY), Math.toRadians(expectedZ));
  assertQuatEquivalent(expected, actual);
}
```

Also test missing rotation -> `Quatd.IDENTITY`, zero source hint/winding, and no mutation of source keyframes.

- [ ] **Step 2: Run RED**

```bash
./gradlew :converter-core:test --tests '*RotationChannelSamplerTest'
```

Expected: compilation failure.

- [ ] **Step 3: Implement scalar Euler segment sampling then quaternion conversion**

Do not route source keyframes through quaternion interpolation. Interpolate the three authored degree components with the same segment/easing progress used for the rotation keyframe, retain the resulting `Vec3d` as `sourceEulerHint`, derive winding deterministically, then call:

```java
Quatd q = Quatd.fromEulerZYX(
    Math.toRadians(sampledEuler.x()),
    Math.toRadians(sampledEuler.y()),
    Math.toRadians(sampledEuler.z()));
```

`previousOutputEuler` stays `Optional.empty()` in T400; final CPM branch selection happens later.

- [ ] **Step 4: Run rotation + math/core regression tests**

```bash
./gradlew :converter-core:test --tests '*RotationChannelSamplerTest'
./gradlew :converter-core:test
```

Expected: PASS, including existing `RotationContinuityTest`/math tests.

- [ ] **Step 5: Commit Task 4**

```bash
git add converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/RotationChannelSampler.java converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/RotationChannelSamplerTest.java
git commit -m "feat: preserve rotation continuity while sampling"
```

---

### Task 5: Orchestrate complete sampled frames across every canonical model bone

**Files:**
- Create: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSampler.java`
- Test: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSamplerTest.java`
- Modify if needed: sampled records from Task 1 only to enforce constructor invariants discovered by tests.

**Interfaces:**
- Consumes: `AnimationClipIR`, `List<BoneId> canonicalBoneOrder`, `SamplingRequest`, `InterpolationEvaluator`.
- Produces:

```java
public final class AnimationSampler {
  public Result<SampledClipIR> sample(
      AnimationClipIR clip,
      List<BoneId> canonicalBoneOrder,
      SamplingRequest request,
      InterpolationEvaluator evaluator);
}
```

- A track lookup is built once by `BoneId`; observable output follows `canonicalBoneOrder`, never map iteration order.

- [ ] **Step 1: Write RED orchestration tests**

Cover the approved full-frame contract:

```java
@Test
void everyFrameContainsEveryCanonicalBoneInOrder() {
  var order = List.of(BODY, HEAD, HORN);
  var result = sampler.sample(clipAnimatingOnlyHead(), order, loop20(), LINEAR_EVALUATOR).value();
  for (var frame : result.frames()) {
    assertEquals(order, frame.bones().stream().map(SampledBoneTransformIR::bone).toList());
  }
}

@Test
void boneWithoutTrackHasIdentityAndNoInventedTrackSemantics() {
  var bone = sampledBone(result, BODY, 0);
  assertEquals(Vec3d.ZERO, bone.transform().translation());
  assertEquals(Quatd.IDENTITY, bone.transform().rotation());
  assertEquals(new Vec3d(1, 1, 1), bone.transform().scale());
  assertTrue(bone.trackSemantics().isEmpty());
}

@Test
void animatedBoneWithMissingChannelsFillsOnlyThoseChannelsWithIdentity() {
  // Position track present, rotation/scale absent; retain track mode/space.
}

@Test
void shuffledTrackInputDoesNotChangeSampledOutput() {
  // Same canonical bone order + logically equivalent track lists/maps => equal SampledClipIR.
}

@Test
void sameClipAt20And40FpsProducesDistinctKeysAndFrameCounts() {
  var a = sample(request(20, LOOP));
  var b = sample(request(40, LOOP));
  assertNotEquals(a.key(), b.key());
  assertNotEquals(a.metadata().frameCount(), b.metadata().frameCount());
}

@Test
void sameClipAndRateWithLoopAndSingleAreNotConflated() {
  assertNotEquals(sample(request(20, LOOP)).key(), sample(request(20, SINGLE)).key());
}
```

Add defensive tests for request clip mismatch, invalid/custom playback classification, duplicate track bone, missing canonical bone referenced by a track, null evaluator, and non-finite sampled result diagnostics.

- [ ] **Step 2: Run RED**

```bash
./gradlew :converter-core:test --tests '*AnimationSamplerTest'
```

Expected: compilation failure/new orchestrator absent.

- [ ] **Step 3: Implement minimal orchestration**

Implementation order per frame:

```java
var grid = TimelineGrid.build(clip.duration(), request.requestedFps(), request.timelineKind());
var tracksByBone = indexTracksOnce(clip.tracks());
for (int frameIndex = 0; frameIndex < grid.times().size(); frameIndex++) {
  double t = grid.times().get(frameIndex);
  var bones = new ArrayList<SampledBoneTransformIR>(canonicalBoneOrder.size());
  for (BoneId boneId : canonicalBoneOrder) {
    BoneTrackIR track = tracksByBone.get(boneId);
    if (track == null) {
      bones.add(identityBone(boneId));
      continue;
    }
    Vec3d position = VectorChannelSampler.sample(track.position(), t, Vec3d.ZERO, evaluator).value();
    RotationSample rotation = RotationChannelSampler.sample(track.rotation(), t, evaluator).value();
    Vec3d scale = VectorChannelSampler.sample(track.scale(), t, new Vec3d(1, 1, 1), evaluator).value();
    bones.add(sampledBone(boneId, track, position, rotation, scale));
  }
  frames.add(new SampledFrameIR(frameIndex, t, bones));
}
```

Do not call `.value()` blindly on failed sub-results in actual code; merge `DiagnosticBag` and return failure if any child evaluator returns an error.

- [ ] **Step 4: Run sampler + all core tests**

```bash
./gradlew :converter-core:test --tests '*AnimationSamplerTest'
./gradlew :converter-core:test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 5**

```bash
git add converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSampler.java converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSamplerTest.java
git commit -m "feat: sample complete T400 animation clips"
```

---

### Task 6: Connect the pinned GeckoLib 4.4.9 easing semantics to the core port

**Files:**
- Create: `adapter-geckolib4/src/main/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluator.java`
- Create: `adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluatorTest.java`
- Reuse without moving: `adapter-geckolib4/src/main/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449EasingEvaluator.java`

**Interfaces:**
- `Gecko449InterpolationEvaluator implements InterpolationEvaluator`.
- Its `evaluate(...)` delegates exactly to `Gecko449EasingEvaluator.apply(...)`.
- `InterpolationIR.CUSTOM` must surface as a stable T400 failure when called through `AnimationSampler`; no linear fallback.

- [ ] **Step 1: Write RED adapter tests**

```java
@Test
void delegatesLinearStepAndPinnedSineValues() {
  InterpolationEvaluator evaluator = new Gecko449InterpolationEvaluator();
  assertEquals(0.5, evaluator.evaluate(InterpolationIR.LINEAR, List.of(), 0.5), 1e-12);
  assertEquals(0.0, evaluator.evaluate(InterpolationIR.STEP, List.of(), 0.5), 1e-12);
  assertEquals(0.2928932188134524,
      evaluator.evaluate(InterpolationIR.EASE_IN_SINE, List.of(), 0.5), 1e-12);
}

@Test
void preservesPinnedEaseInQuintQuirk() {
  assertEquals(0.0625,
      new Gecko449InterpolationEvaluator().evaluate(
          InterpolationIR.EASE_IN_QUINT, List.of(), 0.5), 1e-12);
}
```

Also cover easing args and CUSTOM rejection.

- [ ] **Step 2: Run RED**

```bash
./gradlew :adapter-geckolib4:test --tests '*Gecko449InterpolationEvaluatorTest'
```

Expected: compilation failure.

- [ ] **Step 3: Implement the adapter**

```java
public final class Gecko449InterpolationEvaluator implements InterpolationEvaluator {
  @Override
  public double evaluate(
      InterpolationIR interpolation, List<Double> easingArgs, double normalizedProgress) {
    return Gecko449EasingEvaluator.apply(interpolation, easingArgs, normalizedProgress);
  }
}
```

Keep all Gecko-specific numeric quirks in the existing evaluator.

- [ ] **Step 4: Run adapter regression tests**

```bash
./gradlew :adapter-geckolib4:test --tests '*Gecko449InterpolationEvaluatorTest'
./gradlew :adapter-geckolib4:test --tests '*Gecko449EasingEvaluatorTest'
./gradlew :adapter-geckolib4:test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 6**

```bash
git add adapter-geckolib4/src/main/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluator.java adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449InterpolationEvaluatorTest.java
git commit -m "feat: connect Gecko 4.4.9 easing to sampler"
```

---

### Task 7: Validate samples against existing GeckoLib oracle evidence

**Files:**
- Create: `adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/T400GeckoOracleSamplingTest.java`
- Reuse fixture files under: `spikes/geckolib-animation-semantics/`
- Reuse parser: `adapter-geckolib4/src/main/java/io/github/gabriel0liv/cpmconverter/geckolib4/GeckoAnimationParser.java`
- Reuse existing oracle regression style: `adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/Gecko449OracleFixtureRegressionTest.java`

**Interfaces:**
- Consumes parsed `AnimationClipIR` and `Gecko449InterpolationEvaluator`.
- Produces no new public production API; this is the semantic integration gate for AC-011/AC-013.

- [ ] **Step 1: Select exact oracle cases already committed in the repository**

Use existing S004 artifacts/results to choose at least one committed case each for:

```text
linear
step
non-linear easing
independent channel timestamps
rotation/source Euler where S004 has an asserted expected value
```

Do not create expected numbers from the new implementation. Copy expected numeric values only from the pinned oracle artifacts or independently calculate trivial linear fixtures.

- [ ] **Step 2: Write RED integration assertions**

Structure the test so parser + sampler are both exercised:

```java
@Test
void sampledValuesMatchPinnedGecko449OracleAtKnownTimes() throws Exception {
  AnimationClipIR clip = parseCommittedOracleFixture(...);
  SampledClipIR sampled = new AnimationSampler().sample(
      clip,
      canonicalBones(...),
      new SamplingRequest(clip.id(), requestedFps, timelineKind),
      new Gecko449InterpolationEvaluator()).value();

  assertVec(expectedFromS004, sampledValueAtExactGridTime(sampled, expectedTime), 1e-9);
}
```

Choose FPS/duration so asserted oracle timestamps lie exactly on the generated T400 grid; do not compare an interpolated near-time to an oracle exact-time assertion.

- [ ] **Step 3: Run the new oracle test and require meaningful RED if any T400 semantic gap remains**

```bash
./gradlew :adapter-geckolib4:test --tests '*T400GeckoOracleSamplingTest'
```

If it passes immediately because Tasks 1–6 already satisfy the oracle, record that the test itself was new coverage but do not manufacture a failure by changing correct production behavior. Before accepting immediate GREEN, deliberately perturb one local assertion or use a temporary test expectation to prove the test detects a mismatch, then revert the perturbation before commit.

- [ ] **Step 4: Fix only evidenced semantic mismatches**

Examples of permitted fixes here are exact keyframe-boundary selection or before/after-edge semantics demonstrated by S004. Do not change ADR-004 grid formulas or introduce T401 terminal behavior to satisfy an oracle test.

- [ ] **Step 5: Run parser/easing/oracle/sampler integration suite**

```bash
./gradlew :adapter-geckolib4:test --tests '*T400GeckoOracleSamplingTest'
./gradlew :adapter-geckolib4:test --tests '*Gecko449OracleFixtureRegressionTest'
./gradlew :adapter-geckolib4:test
./gradlew :converter-core:test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 7**

```bash
git add adapter-geckolib4/src/test/java/io/github/gabriel0liv/cpmconverter/geckolib4/T400GeckoOracleSamplingTest.java converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/sampling
git commit -m "test: validate T400 sampling against Gecko oracle"
```

---

### Task 8: Prove determinism, immutability, architecture boundaries, and finish the T400 gate

**Files:**
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/ArchitectureTest.java` if needed to explicitly guard the new sampling package from forbidden dependencies.
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/sampling/AnimationSamplerTest.java`
- Modify: `specs/001-geckolib4-to-cpm/tasks.md`
- Modify: `specs/001-geckolib4-to-cpm/test-plan.md`
- Modify: `specs/001-geckolib4-to-cpm/traceability.md`

**Interfaces:**
- No new public interface; this task freezes the accepted T400 behavior and records evidence.

- [ ] **Step 1: Add regression tests for source immutability and repeatability**

```java
@Test
void samplingDoesNotMutateClipTracksOrKeyframes() {
  AnimationClipIR before = fixtureClip();
  AnimationClipIR snapshot = deepLogicalCopy(before);
  sampler.sample(before, boneOrder, request, evaluator);
  assertEquals(snapshot, before);
}

@Test
void repeatedSamplingProducesEqualLogicalResults() {
  assertEquals(
      sampler.sample(clip, boneOrder, request, evaluator).value(),
      sampler.sample(clip, boneOrder, request, evaluator).value());
}
```

If a `deepLogicalCopy` helper would itself be complex, construct the expected immutable source records once and assert their keyframe/value lists exactly after sampling; do not add serialization only for this test.

- [ ] **Step 2: Run focused tests and observe RED only if a real missing invariant is discovered**

```bash
./gradlew :converter-core:test --tests '*AnimationSamplerTest'
```

Fix any actual mutation/order issue before proceeding.

- [ ] **Step 3: Run the full local repository gate**

On Linux/macOS shell:

```bash
./gradlew --no-daemon --console=plain spotlessCheck clean check
python scripts/verify-reproducible-build.py
python test-fixtures/scripts/manifest.py --check
python spikes/geckolib-animation-semantics/scripts/audit_fixtures.py
```

On Windows CMD/PowerShell use the corresponding `gradlew.bat` command:

```bat
.\gradlew.bat --no-daemon --console=plain spotlessCheck clean check
python scripts/verify-reproducible-build.py
python test-fixtures/scripts/manifest.py --check
python spikes/geckolib-animation-semantics/scripts/audit_fixtures.py
```

Expected: all commands exit 0.

- [ ] **Step 4: Commit production/tests before claiming completion**

```bash
git add converter-core adapter-geckolib4 converter-config
git commit -m "test: harden T400 sampler determinism"
```

Skip this commit if there are no uncommitted code/test changes after the previous task.

- [ ] **Step 5: Push the implementation branch and verify GitHub Actions Ubuntu + Windows**

Use the repository's existing CI. Require every normal `check` job and every existing verification/oracle job to complete successfully. Record the exact run ID in docs; do not cite an older green run.

- [ ] **Step 6: Update T400 documentation only after the fresh green CI run**

Change `tasks.md`:

```markdown
- [x] T400 sampler 20 fps/config (FR-014/015) — <concise evidence>; gate Ubuntu/Windows verde no run <RUN_ID>.
```

Update `test-plan.md` and `traceability.md` so FR-014/FR-015 and AC-010/011/013/017 point to the concrete T400 tests introduced above. State explicitly that loop seam/hold remains T401 and CPM projection/state mapping remains T402.

- [ ] **Step 7: Run docs-sensitive/full gate once more**

```bash
./gradlew --no-daemon --console=plain spotlessCheck clean check
```

Expected: PASS.

- [ ] **Step 8: Commit the T400 closure evidence**

```bash
git add specs/001-geckolib4-to-cpm/tasks.md specs/001-geckolib4-to-cpm/test-plan.md specs/001-geckolib4-to-cpm/traceability.md
git commit -m "docs: close T400 animation sampling"
```

- [ ] **Step 9: Verify the closure commit CI before declaring T400 `[x]` externally**

Wait for the CI run triggered by the closure commit and verify all required jobs are green. If docs-only changes trigger no CI in repository policy, cite the immediately preceding implementation run plus the locally rerun gate and make that limitation explicit.

---

## Plan Self-Review

### Spec coverage

- Sampler boundary/no CPM/no hierarchy: Tasks 1 and 5.
- FPS precedence/default/multi-rate key: Tasks 1, 2, and 5.
- ADR-004 LOOP/SINGLE grids and metadata: Task 1.
- All bones/identity completion/ordering: Task 5.
- Independent position/rotation/scale timestamps: Tasks 3, 4, 5, and 7.
- Gecko easing port and no core Gecko dependency: Tasks 3 and 6.
- Continuous Euler/winding/no SLERP: Task 4.
- `pre/post` stays parser-owned: Task 7 consumes parsed IR only and never reopens raw JSON semantics.
- Stable diagnostics/non-finite/custom failure: Tasks 3, 5, and 6.
- Determinism/immutability/Windows+Linux: Task 8.
- T401/T402 exclusions: Tasks 5, 7, and closure docs in Task 8.

### Placeholder scan

The plan intentionally contains no `TBD`, `TODO`, generic “handle edge cases”, or unnamed tests. Oracle values that must come from existing S004 evidence are selected during Task 7 from committed artifacts rather than invented; the selection procedure and required categories are explicit.

### Type consistency

The downstream tasks consistently use `SamplingRequest`, `SampledClipKey`, `SamplingMetadataIR`, `SampledBoneTransformIR`, `SampledFrameIR`, `SampledClipIR`, `InterpolationEvaluator`, `TimelineGrid`, `VectorChannelSampler`, `RotationChannelSampler`, and `AnimationSampler` with the signatures introduced in the earliest task that owns each type.
