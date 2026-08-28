# T400 — Animation Sampler Design

Status: **DESIGN APPROVED — USER REVIEW OF WRITTEN SPEC PENDING**

Date: 2026-08-28

## Goal

Implement the first production animation-sampling subsystem for the converter. T400 evaluates parsed GeckoLib 4.4.9 animation tracks on a deterministic uniform temporal grid and produces an intermediate sampled representation that later phases can consume.

T400 does **not** write CPM animations, map clips to vanilla poses, apply bind transforms, compose hierarchy/world transforms, perform look/head retargeting, or decide loop seam/hold terminal behavior. Those remain in T401/T402/T500.

## Existing contracts

This design preserves the already accepted contracts in:

- `docs/decisions/ADR-004-animation-sampling.md`;
- `specs/001-geckolib4-to-cpm/data-model.md`;
- `specs/001-geckolib4-to-cpm/requirements.md` (`FR-014`, `FR-015`);
- `specs/001-geckolib4-to-cpm/acceptance-criteria.md` (`AC-010`, `AC-011`, `AC-013`, `AC-017`);
- the existing `CompiledSamplingPolicy` and per-state `requestedFps` configuration;
- the GeckoLib 4.4.9 easing evaluator already implemented in `adapter-geckolib4`.

The existing `AnimationClipIR`, `BoneTrackIR`, `ChannelIR`, `SourceRotationChannelIR`, and source-authored continuous Euler policy remain authoritative input semantics.

## Architectural boundary

The sampler belongs in `converter-core` because temporal sampling, frame grids, identity completion, sample ordering, metadata, and rotation continuity are converter-domain concerns rather than Gecko-specific parsing concerns.

Gecko-specific easing behavior remains in `adapter-geckolib4` behind a narrow evaluator port. This avoids coupling the core sampler to GeckoLib classes while still reproducing the pinned GeckoLib 4.4.9 numeric behavior.

Conceptual flow:

```text
AnimationClipIR
      +
SamplingRequest
      +
InterpolationEvaluator
      +
canonical model bone order
      ↓
AnimationSampler
      ↓
SampledClipIR
  ├─ SampledClipKey
  ├─ SamplingMetadataIR
  └─ SampledFrameIR[]
       └─ BoneId → SampledBoneTransformIR
```

No Minecraft, Forge, GeckoLib, CPM, or Blockbench runtime dependency is introduced into `converter-core`.

## Sampling request and FPS resolution

Configuration precedence is normative:

```text
stateMapping.requestedFps
    → sampling.requestedFps
    → 20
```

Allowed FPS remains `1..240` as already enforced by configuration validation.

The same source clip may legitimately be requested at multiple frame rates. Therefore sampling results must not be cached or indexed solely by `ClipId`.

Use the conceptual key:

```text
SampledClipKey(
    ClipId clipId,
    int requestedFps,
    TimelineKind timelineKind
)
```

`TimelineKind` is part of the key because a loop grid and a single-shot grid place frames at different times even for the same duration and FPS.

A state-specific FPS override produces a distinct sampled result rather than mutating or replacing the global-rate result.

## Timeline kinds

T400 reduces playback to the temporal-grid distinction required for sampling:

```text
PlaybackMode.LOOP      → TimelineKind.LOOP
PlaybackMode.PLAY_ONCE → TimelineKind.SINGLE
PlaybackMode.HOLD      → TimelineKind.SINGLE
```

`PlaybackMode.CUSTOM` is not silently coerced. If a supported custom mode cannot be classified by an explicit upstream rule, T400 returns an actionable diagnostic/error rather than guessing.

T400 does not implement terminal `PLAY_ONCE` vs `HOLD` behavior. That difference belongs to T401.

## Frame count and timestamps

For duration `D > 0` and requested rate `F`:

```text
N = max(1, round(D × F))
```

Java rounding semantics must be made explicit in implementation tests so cross-platform behavior is not inferred from locale or formatting.

### LOOP

For a loop timeline:

```text
frameCount = N
frameInterval = D / N
effectiveIntervalRate = N / D
frameDensity = N / D
t_i = i × D / N
for i = 0..N-1
```

The sampler does not emit a duplicate frame at `t=D`; that instant belongs to the next loop cycle.

### SINGLE

For a single timeline with `N >= 2`:

```text
frameCount = N
frameInterval = D / (N - 1)
effectiveIntervalRate = (N - 1) / D
frameDensity = N / D
t_i = i × D / (N - 1)
for i = 0..N-1
```

The final frame is exactly `t=D`.

For `N = 1`:

```text
t_0 = 0
frameInterval = 0
effectiveIntervalRate = 0
frameDensity = 1 / D
```

### Metadata

Every `SampledClipIR` records:

- `requestedFps`;
- `frameCount`;
- `frameDensity`;
- `effectiveIntervalRate`;
- `frameInterval`;
- `maxTemporalGridError`.

`maxTemporalGridError` is:

```text
max_i |t_i - i / requestedFps|
```

The ambiguous label `effectiveFps` is forbidden by ADR-004 and must not reappear in API, report, tests, or docs.

## Sampled output model

T400 introduces/finishes a dedicated sampled-animation IR in `converter-core`.

Conceptually:

```text
SampledClipIR
  key: SampledClipKey
  sourceDurationSeconds: double
  sourcePlayback: PlaybackMode
  metadata: SamplingMetadataIR
  frames: List<SampledFrameIR>

SampledFrameIR
  index: int
  timeSeconds: double
  bones: ordered collection of SampledBoneTransformIR

SampledBoneTransformIR
  bone: BoneId
  transform: SampledTransformIR
  mode: TransformMode
  space: TransformSpace

SampledTransformIR
  translation: Vec3d
  rotation: Quatd
  scale: Vec3d
  rotationContinuity: RotationContinuityIR
```

Names may be adjusted during the implementation plan to match existing repository naming conventions, but the semantics above are normative.

## All bones are present in every frame

Every `SampledFrameIR` contains **all model bones in canonical order**, not only animated bones.

For a bone without a track:

```text
translation = (0, 0, 0)
rotation = identity quaternion
scale = (1, 1, 1)
```

For an animated bone with a missing channel, only that channel receives identity:

- missing position → `(0, 0, 0)`;
- missing rotation → identity quaternion with neutral continuity state;
- missing scale → `(1, 1, 1)`.

This keeps downstream T401/T402 deterministic and prevents absence from carrying two meanings (`not animated` vs `forgotten`).

The sampler does not search bones by source name. It consumes resolved `BoneId` values and a canonical bone-order input.

## What a sampled transform means

A T400 sample is the **evaluated animation transform/delta for that bone at that instant**.

It is deliberately not a final pose and does not include:

- `BoneIR.bindLocal`;
- parent transforms;
- world-space composition;
- CPM root/anchor transforms;
- model scale or vertical offset;
- look/head layers;
- retargeting;
- CPM serialization conventions.

`TransformMode` (`ABSOLUTE` / `ADDITIVE`) and `TransformSpace` (`LOCAL` / `MODEL`) remain explicit metadata so the later composition phase can interpret the sampled values correctly.

This boundary prevents temporal evaluation from becoming entangled with hierarchy/retarget math.

## Channel evaluation

Position, rotation, and scale are evaluated independently because Gecko animations may use different timestamps and interpolation modes per channel.

At each frame time the sampler finds the effective segment for each present channel, evaluates its interpolation/easing semantics, and returns a value at that exact instant.

No common-source-keyframe assumption is allowed.

### Boundary behavior

Keyframe-boundary behavior must be pinned by tests against the existing parser/oracle evidence. Implementations must not use an epsilon to silently move a timestamp to a neighboring segment unless an explicit compatibility rule documents it.

Before the first keyframe and after the last keyframe, behavior must match the pinned GeckoLib 4.4.9 evaluation semantics demonstrated by repository oracle fixtures. T400 must not invent generic animation-library behavior when Gecko evidence differs.

## Interpolation evaluator port

`converter-core` owns the sampling algorithm but does not own GeckoLib easing formulas.

Introduce a narrow core-facing port conceptually equivalent to:

```text
InterpolationEvaluator.evaluate(
    interpolation,
    easingArgs,
    normalizedProgress,
    segmentContext
) -> evaluated progress/value support
```

The exact method shape is chosen during the implementation plan so CATMULLROM can receive whatever neighbor context is actually required without leaking GeckoLib classes into the core.

`adapter-geckolib4` supplies the GeckoLib 4.4.9 implementation using the already pinned numeric semantics.

Built-ins supported by the existing adapter remain supported. `CUSTOM` remains unsupported offline and must fail explicitly rather than degrade to linear.

## Position and scale

For ordinary vector channels:

1. select the effective source segment at sample time;
2. compute normalized temporal progress without quantizing the sample time;
3. evaluate the segment interpolation/easing according to the pinned adapter;
4. interpolate `incomingValue` / `outgoingValue` in `double` precision;
5. return the sampled `Vec3d`.

Scale remains animation-channel scale/delta according to track metadata. T400 does not combine it with bind scale.

## Rotation and continuity

Rotation sampling preserves the source-authored continuous Euler contract.

The normative path is:

```text
SourceRotationChannelIR
  → select exact temporal segment
  → use effective incoming/outgoing Euler values
  → evaluate interpolation/easing in continuous scalar Euler space
  → produce sampled source Euler without normalization
  → preserve source Euler hint/winding
  → convert that instantaneous orientation to ZYX quaternion
  → emit SampledTransformIR + RotationContinuityIR
```

T400 must **not**:

- normalize authored values into `[-180°, 180°]`;
- replace `360°`, `720°`, etc. with equivalent zero-angle orientations before recording continuity;
- use SLERP between source keyframes;
- independently choose shortest quaternion paths per frame.

Explicit authored winding such as:

```text
0° → 360° → 720°
```

must survive sampling as continuity information even though the instantaneous quaternions at multiples of 360° are orientation-equivalent.

Crossings such as `+179° → -179°` retain the source-authored hint at this stage. Final CPM Euler branch selection remains a later boundary operation.

## `pre` / `post` compatibility

T400 consumes the IR already normalized to the pinned GeckoLib 4.4.9 compatibility rule.

The parser contract remains:

- use `pre` when present;
- use `post` only when `pre` is absent;
- when both exist and differ, parser emits `ANIM_PRE_POST_COLLAPSED_449`.

T400 does not reinterpret raw Bedrock `pre/post` semantics or inspect source JSON again.

## Determinism

Sampling is deterministic for identical logical inputs and configuration.

Required rules:

- all math uses `double`;
- frame times are derived from `D`, `N`, and frame index, not accumulated by repeated `time += interval`;
- frame order is ascending index;
- bone order is the canonical model bone order supplied to the sampler;
- no observable `HashMap` iteration order;
- no locale-sensitive parsing/formatting in logical results;
- no absolute paths or timestamps in sampled logical data;
- sampling does not mutate `ModelIR`, channels, keyframes, or bind transforms.

Ubuntu and Windows tests must produce logically identical sampled results and stable golden serialization where golden serialization is used for tests.

## Validation and diagnostics

T400 assumes parser/IR validation has already established basic validity, but must reject impossible sampling requests defensively.

At minimum, errors include:

- missing/null clip or bone-order input;
- requested FPS outside `1..240` if an invalid request bypasses config validation;
- non-finite/invalid duration reaching the sampler;
- `CUSTOM` interpolation/easing that has no offline evaluator;
- playback that cannot be mapped to a supported timeline kind;
- malformed channel state that makes segment evaluation undefined;
- non-finite sampled output from interpolation/easing.

Diagnostics must use stable repository diagnostic codes. If no suitable code exists, T400 adds narrowly scoped `ANIM_*` codes rather than throwing user-facing generic exceptions.

Internal programmer-contract violations may still throw only where repository conventions already treat them as impossible internal errors.

## Scope split with T401

T400 is responsible for **sampling one temporal cycle/domain correctly**.

T401 owns playback continuity semantics:

- loop seam measurement;
- `ANIM_LOOP_DISCONTINUITY` policy;
- terminal `PLAY_ONCE` behavior;
- terminal `HOLD` behavior;
- any explicit wrap/terminal frame transformation needed for CPM projection.

T400 may expose source playback and timeline metadata needed by T401 but must not implement these policies itself.

## Scope split with T402

T402 consumes `SampledClipIR` and owns CPM animation projection:

- mapping state names/VanillaPose to clips;
- choosing CPM animation filenames;
- converting sampled bone values into CPM component/frame structures;
- applying the required bind/hierarchy/projection-space interpretation;
- serializing/interpolator semantics required by CPM;
- allowing the same source clip to be projected from different `SampledClipKey` variants when state FPS differs.

No CPM file or animation JSON is generated by T400.

## Testing strategy

T400 is implemented test-first.

### Unit tests — temporal grid

Cover at least:

- default 20 FPS resolution;
- `1` and `240` FPS boundaries;
- durations where `D × F` is below, above, and exactly at half-rounding boundaries;
- loop `N=1` and `N>1`;
- single `N=1`, `N=2`, and larger;
- exact `t=0` and exact `t=D` inclusion/exclusion rules;
- metadata calculations including `maxTemporalGridError`;
- no cumulative floating-point drift from iterative time addition.

### Unit tests — identity completion and ordering

Cover:

- all bones emitted in canonical order;
- completely unanimated bone → identity transform;
- missing position only;
- missing rotation only;
- missing scale only;
- source track metadata preserved;
- input maps deliberately shuffled without changing sampled logical output.

### Unit tests — interpolation

Cover:

- linear;
- step;
- at least one non-linear easing required by AC-011;
- easing arguments;
- keyframe boundary times;
- independent timestamps across position/rotation/scale;
- first/last segment behavior;
- explicit rejection of unsupported custom interpolation.

### Rotation tests

Cover:

- ordinary Euler interpolation;
- non-commuting multi-axis sample converted ZYX;
- `0° → 360° → 720°` preservation of source continuity/winding;
- `+179° → -179°` preservation of source hint without normalization;
- missing rotation identity;
- no mutation of source rotation keyframes.

### Configuration/key tests

Cover:

- state FPS overrides global FPS;
- global FPS overrides default;
- default is exactly 20;
- same clip sampled at 20 and 40 FPS yields two different `SampledClipKey` values/results;
- same clip/rate with different timeline kind is not conflated.

### Oracle/integration tests

Use existing GeckoLib 4.4.9 oracle fixtures to compare sampled values at exact known timestamps for:

- linear;
- step;
- a non-linear easing;
- independent channels/timestamps;
- source rotation values where available.

The target numeric tolerances remain those already documented for animation samples unless an ADR explicitly changes them.

### Regression tests

Prove that sampling:

- does not mutate bind transforms;
- does not mutate source keyframes;
- is repeatable over repeated calls;
- remains deterministic in Ubuntu/Windows CI;
- leaves T401/T402 behavior out of scope rather than implicitly baking it into samples.

## Acceptance for T400

T400 can move to `[x]` when all of the following are true:

1. the sampled IR and sampler are implemented in production code with no forbidden external runtime dependency in `converter-core`;
2. FPS resolution and multi-rate sampling follow the approved precedence/key rules;
3. loop/single temporal grids and metadata exactly follow ADR-004;
4. every frame contains every model bone in canonical order with identity completion;
5. position/rotation/scale channels sample independently and correctly;
6. supported GeckoLib 4.4.9 interpolation/easing matches the pinned oracle within tolerance;
7. rotation continuity/winding tests pass without pre-sampling normalization or SLERP;
8. source IR/bind state remains immutable across sampling;
9. normal repository checks, fixture/oracle gates, reproducibility checks, Ubuntu CI, and Windows CI are green;
10. T400 does not claim T401 loop/hold semantics or T402 CPM animation projection as completed.

## Explicitly out of scope

The following are not implementation requirements for T400:

- loop seam correction or warning policy;
- terminal hold/play-once runtime behavior;
- CPM animation filenames/poses;
- CPM animation writer changes;
- state mapping projection;
- look/yaw/pitch layering;
- head/neck retargeting;
- bind/world hierarchy composition;
- model-scale/vertical-offset application;
- adaptive sampling;
- sample reduction/compression;
- error-based automatic FPS selection;
- dynamic Molang;
- arbitrary custom Gecko easing callbacks.

These exclusions are intentional to keep temporal sampling independently testable and to preserve the phase boundaries already documented in the project roadmap.
