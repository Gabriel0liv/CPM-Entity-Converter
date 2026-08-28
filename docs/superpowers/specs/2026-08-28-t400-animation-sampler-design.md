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
       └─ SampledBoneTransformIR[]
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

`PlaybackMode.CUSTOM` is not silently coerced. T400 rejects it with a stable `ANIM_*` diagnostic unless a future explicit compatibility rule first maps that custom mode to a supported timeline kind.

T400 does not implement terminal `PLAY_ONCE` vs `HOLD` behavior. That difference belongs to T401.

## Frame count and timestamps

For duration `D > 0` and requested rate `F`:

```text
N = max(1, round(D × F))
```

Implementation uses Java `Math.round(double)` semantics. Tests pin half-rounding cases explicitly so cross-platform behavior is not inferred from locale or formatting.

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
  bones: List<SampledBoneTransformIR>

SampledBoneTransformIR
  bone: BoneId
  transform: SampledTransformIR
  trackSemantics: Optional<SampledTrackSemanticsIR>

SampledTrackSemanticsIR
  mode: TransformMode
  space: TransformSpace

SampledTransformIR
  translation: Vec3d
  rotation: Quatd
  scale: Vec3d
  rotationContinuity: RotationContinuityIR
```

The implementation plan may adapt class names to existing repository naming conventions, but these fields and semantics are normative.

`trackSemantics` is present only when the source clip has a `BoneTrackIR` for that bone. It is absent for a completely unanimated bone. This avoids inventing `ABSOLUTE/ADDITIVE` or `LOCAL/MODEL` semantics for bones that had no source track.

## All bones are present in every frame

Every `SampledFrameIR` contains **all model bones in canonical pre-order**, not only animated bones.

The sampler receives an explicit canonical bone-order list derived from the validated model. It validates that the list contains every model bone exactly once; duplicate, missing, or unknown IDs are an error rather than an ordering fallback.

For a bone without a track:

```text
translation = (0, 0, 0)
rotation = identity quaternion
scale = (1, 1, 1)
rotationContinuity = neutral
trackSemantics = empty
```

Neutral rotation continuity means:

```text
sourceEulerHint = (0, 0, 0)
winding = (0, 0, 0)
previousOutputEuler = empty
```

For an animated bone with a missing channel, only that channel receives identity:

- missing position → `(0, 0, 0)`;
- missing rotation → identity quaternion + neutral rotation continuity;
- missing scale → `(1, 1, 1)`.

The source track's `TransformMode` and `TransformSpace` remain present even if only one of its channels exists.

This keeps downstream T401/T402 deterministic and prevents absence from carrying two meanings (`not animated` vs `forgotten`).

The sampler does not search bones by source name. It consumes resolved `BoneId` values.

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

`TransformMode` (`ABSOLUTE` / `ADDITIVE`) and `TransformSpace` (`LOCAL` / `MODEL`) remain explicit source-track metadata so the later composition phase can interpret the sampled values correctly.

This boundary prevents temporal evaluation from becoming entangled with hierarchy/retarget math.

## Channel evaluation

Position, rotation, and scale are evaluated independently because Gecko animations may use different timestamps and interpolation modes per channel.

At each frame time the sampler finds the effective segment for each present channel, evaluates its interpolation/easing semantics, and returns a value at that exact instant.

No common-source-keyframe assumption is allowed.

### Boundary behavior

Keyframe-boundary behavior must be pinned by tests against the existing parser/oracle evidence. Implementations must not use an epsilon to silently move a timestamp to a neighboring segment unless an explicit compatibility rule documents it.

Before the first keyframe and after the last keyframe, behavior must match the pinned GeckoLib 4.4.9 evaluation semantics demonstrated by repository oracle fixtures. T400 must not invent generic animation-library behavior when Gecko evidence differs.

A channel with zero keyframes is treated as absent/identity only if validated IR construction permits that state; otherwise the validator/sampler rejects the malformed channel explicitly. A channel with one keyframe follows pinned GeckoLib single-keyframe behavior, covered by a dedicated test rather than an inferred generic rule.

## Interpolation evaluator port

`converter-core` owns segment selection, timestamp math, endpoint selection, and vector interpolation. It does not own GeckoLib easing formulas.

The core-facing port is semantically:

```text
double apply(
    InterpolationIR interpolation,
    List<Double> easingArgs,
    double normalizedProgress
)
```

`normalizedProgress` supplied by the core is finite and within `[0,1]` for a selected segment. The adapter may return values outside `[0,1]` for overshooting/quirky Gecko easings such as back, elastic, bounce, or the pinned CATMULLROM registration behavior. T400 must **not clamp the evaluated easing result**.

`adapter-geckolib4` implements this port by delegating to the already tested GeckoLib 4.4.9 numeric evaluator. The core then applies the returned scalar factor component-wise between the effective segment endpoints.

Built-ins supported by the existing adapter remain supported. `CUSTOM` remains unsupported offline and fails explicitly rather than degrading to linear.

## Position and scale

For ordinary vector channels:

1. select the effective source segment at sample time;
2. compute normalized temporal progress without quantizing the sample time;
3. evaluate the segment interpolation/easing through the adapter port;
4. interpolate `incomingValue` / `outgoingValue` component-wise in `double` precision using the returned factor;
5. return the sampled `Vec3d`.

Scale remains animation-channel scale/delta according to track metadata. T400 does not combine it with bind scale.

## Rotation and continuity

Rotation sampling preserves the source-authored continuous Euler contract.

The normative path is:

```text
SourceRotationChannelIR
  → select exact temporal segment
  → use effective incoming/outgoing Euler values
  → evaluate interpolation/easing in continuous Euler component space
  → produce sampled source Euler without normalization
  → derive source Euler hint/winding from that sampled Euler
  → convert that instantaneous orientation to ZYX quaternion
  → emit SampledTransformIR + RotationContinuityIR
```

For each axis, winding is the integer multiple-of-360 branch carried by the sampled source Euler relative to its principal equivalent. The implementation must use the same deterministic branch convention across platforms and test it explicitly at exact multiples and negative rotations.

T400 must **not**:

- normalize authored values into `[-180°, 180°]` before recording continuity;
- replace `360°`, `720°`, etc. with equivalent zero-angle orientations before recording continuity;
- use SLERP between source keyframes;
- independently choose shortest quaternion paths per frame.

Explicit authored winding such as:

```text
0° → 360° → 720°
```

must survive sampling as continuity information even though the instantaneous quaternions at multiples of 360° are orientation-equivalent.

Crossings such as `+179° → -179°` retain the sampled source Euler as the authorial hint at this stage. T400 does not rewrite that authored path into a quaternion-shortest path. Final CPM Euler branch selection remains a later boundary operation.

`previousOutputEuler` remains empty in T400 because no CPM Euler output branch has been selected yet. Later phases may populate it when crossing the CPM representation boundary.

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
- bone order is the validated canonical model pre-order supplied to the sampler;
- no observable `HashMap` iteration order;
- no locale-sensitive parsing/formatting in logical results;
- no absolute paths or timestamps in sampled logical data;
- sampling does not mutate `ModelIR`, channels, keyframes, or bind transforms.

Ubuntu and Windows tests must produce logically identical sampled results and stable golden serialization where golden serialization is used for tests.

## Validation and diagnostics

T400 assumes parser/IR validation has already established basic validity, but rejects impossible sampling requests defensively.

At minimum, errors include:

- missing/null clip or model/bone-order input;
- canonical order with duplicate, missing, or unknown `BoneId` values;
- requested FPS outside `1..240` if an invalid request bypasses config validation;
- non-finite/invalid duration reaching the sampler;
- `CUSTOM` interpolation/easing that has no offline evaluator;
- playback that cannot be mapped to a supported timeline kind;
- malformed channel state that makes segment evaluation undefined;
- non-finite normalized progress, easing result, or sampled transform component.

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

T400 exposes source playback and timeline metadata needed by T401 but does not implement these policies itself.

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

- all bones emitted in canonical pre-order;
- canonical order must cover every model bone exactly once;
- completely unanimated bone → identity transform + empty track semantics;
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
- overshooting easing result is not clamped;
- easing arguments;
- pinned CATMULLROM evaluator behavior;
- keyframe boundary times;
- independent timestamps across position/rotation/scale;
- zero/single-keyframe behavior according to validated Gecko semantics;
- first/last segment behavior;
- explicit rejection of unsupported custom interpolation.

### Rotation tests

Cover:

- ordinary Euler interpolation;
- non-commuting multi-axis sample converted ZYX;
- `0° → 360° → 720°` preservation of source continuity/winding;
- negative and exact-multiple winding branch convention;
- `+179° → -179°` preservation of source hint without pre-sampling normalization;
- `previousOutputEuler` empty in T400;
- missing rotation identity + neutral continuity;
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
4. every frame contains every model bone in canonical pre-order with identity completion and unambiguous optional track semantics;
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
