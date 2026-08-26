# T300 — Static CPM projection design

Status: **approved and compatibility-reviewed against pinned CPM 0.6.27 / GeckoLib 4.4.9; implementation waits for T205 source-feature hardening**.

Date: 2026-08-26.
Branch: `agent/correct-look-retargeting-phase1`.

## Goal

T300 creates the first production boundary from `ModelIR` to a CPM-specific static project graph. The output is **not yet a `.cpmproject` ZIP** and does not assign final CPM `storeID` values. It creates a deterministic, writer-owned representation of CPM roots, structural elements, cube helpers and renderable cubes while preserving source hierarchy, bind transforms, cube ownership, cube pivots/rotations and global projection settings.

The task implements FR-011/FR-012 and prepares FR-007/FR-010/FR-026. It must not pull animation, dynamic look retargeting, ZIP serialization, final numeric IDs or CLI concerns forward from T301/T302/T400/T500.

Structural correctness is mandatory in T300. The topology must also remain suitable for later animation and semantic correctness without being replaced.

## Chosen architecture

Use **single-anchor projection with stable structural bone nodes**.

```text
CPM HEAD       vanilla geometry hidden
CPM BODY       vanilla geometry hidden
└── entity_root
    ├── gecko_root_A
    │   ├── bone...
    │   │   ├── cube
    │   │   └── cube_pivot_helper
    │   │       └── cube
    │   └── ...
    └── gecko_root_B...
CPM LEFT_ARM   vanilla geometry hidden
CPM RIGHT_ARM  vanilla geometry hidden
CPM LEFT_LEG   vanilla geometry hidden
CPM RIGHT_LEG  vanilla geometry hidden
```

Every Gecko bone becomes exactly one structural CPM element even when it owns zero or one cube. A logical bone such as `body`, `neck`, `head`, `jaw` or an accessory therefore keeps a stable target independent of cube count. T400/T500 can animate or retarget that bone without topology changing when geometry changes.

This intentionally rejects the compact strategy where a one-cube bone is represented directly by the cube element. The additional structural nodes preserve identity, hierarchy and editability and reduce later retarget complexity.

The pinned CPM exporter independently demonstrates the same underlying mechanism for group-only structure: a group with no cube becomes a normal `ModelElement` with zero size. T300 keeps that mechanism consistently for every source bone instead of compacting one-cube groups.

## Compatibility review against the real runtimes

The architecture was reviewed against CPM `0.6.27` commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` and GeckoLib `4.4.9` commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.

The review confirms:

- CPM normal elements can safely serve as transform-only hierarchy nodes when they have zero size and no texture geometry;
- CPM applies normal-element position, rotation and `rscale` to the matrix before descending into children, so `entity_root` is a valid inherited transform boundary;
- the official CPM Blockbench converter itself creates transform groups for rotated cubes, matching the helper-node strategy instead of flattening cube rotation;
- the observed CPM root conversion `(-x, 24-y, +z)` is exactly reproduced by the parser's existing root local `(-x,-y,+z)` under one `(0,24,0)` `entity_root`;
- humanoid, deep-hierarchy and quadruped fixtures remain representable by the same topology because arbitrary Gecko roots stay below one BODY anchor rather than being forced into humanoid root partitions.

The review also found compatibility constraints that are normative below:

1. structural elements must remain runtime-visible (`hidden=false`) even though they have no mesh; using CPM `hidden` for them can suppress descendant rendering;
2. CPM clamps inherited render scale to a minimum of `0.01`, so `0 < modelScale < 0.01` is not exactly representable and must be rejected rather than silently clamped;
3. CPM V1 stores UV coordinates/sizes in integer fields, while the Gecko adapter correctly preserves signed/fractional `double` UV. T300 must not quantize those values; exact serializability is checked by T302/T303 under the no-PNG-reencode MVP policy;
4. the pinned Gecko raw bone format recognizes source metadata that the current parser does not yet preserve or reject (`bind_pose_rotation`, `debug`, `locators`, bone `mirror`, `neverRender`, `render_group_id`, `reset`, `texture_meshes`). T205 must close that NFR-007 gap before T300 production implementation consumes arbitrary real-world `ModelIR` input.

The fourth item is a parser/subset-hardening issue, not a failure of the single-anchor topology. Cube raw fields used by the Gecko 4.4.9 baked model (`inflate`, `mirror`, `origin`, `pivot`, `rotation`, `size`, `uv`) are already represented by the current cube IR path.

## Module boundary

`writer-cpm` remains dependent only on `converter-core`.

T300 must **not** add a dependency from `writer-cpm` to `converter-config`. Instead, `writer-cpm` owns a small already-resolved input, conceptually:

```text
CpmProjectionSettings
  modelScale: double
  verticalOffset: double
  anchor: BODY
  hideVanillaRoots: boolean
  disableVanillaAnim: boolean
```

For the MVP, `anchor` is BODY. `modelScale` defaults to `1`, `verticalOffset` to `0`, and production replacement conversion requests `hideVanillaRoots=true`.

`disableVanillaAnim` has **no hidden projector default**. The caller passes the intended value explicitly. This keeps T300 capable of representing the flag without deciding the still-pending visual policy from S001/S002/T500/T701.

The later orchestration layer translates `SemanticRigMap` into `CpmProjectionSettings`. Config-schema classes do not leak into the writer module.

## CPM project graph owned by `writer-cpm`

T300 introduces a CPM-specific graph separate from `ModelIR`. It carries enough information for T301/T302 without serializing JSON prematurely.

Conceptual model:

```text
CpmStaticProjectV1
  roots: ordered list<CpmRootV1>
  texture metadata/reference
  logicalTargets: immutable ordered map<ProjectionKey, node>

CpmRootV1
  vanillaPart: HEAD | BODY | LEFT_ARM | RIGHT_ARM | LEFT_LEG | RIGHT_LEG
  showVanillaGeometry: boolean
  disableVanillaAnim: boolean
  transform
  children

CpmElementV1
  key: ProjectionKey
  name
  kind: ENTITY_ROOT | BONE | CUBE_HELPER | CUBE
  transform
  structuralVisibility
  cube/appearance when kind == CUBE
  children
```

`ProjectionKey` is deterministic logical identity, not final numeric CPM identity. Required key domains are:

- `ENTITY_ROOT`
- `BONE:<BoneId>`
- `CUBE_HELPER:<CubeId>`
- `CUBE:<CubeId>`

T301 consumes this graph and assigns numeric `storeID` values in canonical preorder. T300 therefore exposes every later animation target through a stable logical key and preserves deterministic child order.

## Vanilla roots

CPM has six vanilla player roots independently of converted custom children. An entity conversion is intended to replace the player body, not render on top of Steve/Alex, so the graph explicitly represents all six roots and supports hiding their vanilla geometry.

Only BODY receives `entity_root`. HEAD, arms and legs remain valid CPM roots but receive no Gecko subtree in the single-anchor architecture.

`showVanillaGeometry` and `disableVanillaAnim` are separate properties. T300 represents both; it does not infer that hiding vanilla geometry automatically means disabling vanilla transform inheritance.

The exact runtime/editor behavior of hidden vanilla roots with converted descendants remains part of T304/T701 visual/conformance acceptance. If that gate shows that a different CPM field combination is required to hide vanilla geometry while keeping descendants, T302/T304 may adjust serialization semantics without changing the T300 single-anchor topology.

## Structural element visibility

`entity_root`, every `BONE` node and every `CUBE_HELPER` are transform-only **normal CPM elements**, not hidden elements.

Their required CPM-facing structural state is:

```text
size        = (0,0,0)
texture     = false
show        = true
hidden      = false
```

They are visually empty because their size is zero, not because rendering is disabled.

This distinction is mandatory. In the pinned CPM renderer, a normal child with `display=false`/hidden recurses with element rendering disabled for its descendants. Marking a transform-only parent hidden could therefore make otherwise valid descendant cubes disappear. Vanilla root geometry hiding is a separate root-level concern and must not be reused for structural normal elements.

## `entity_root` and global projection boundary

`entity_root` is synthetic and geometry-free. It isolates player/CPM root-space concerns from the original Gecko hierarchy.

The parser stores a root-bone bind translation from the source pivot as:

```text
C(rootPivot) = (-x, -y, +z)
```

The observed CPM/Blockbench root conversion uses a player-space Y baseline of 24 pixels. Under BODY single-anchor, the baseline and global conversion adjustments belong on `entity_root` rather than being baked independently into every Gecko root.

Normative transform:

```text
M_entityRoot = T(0, 24 + verticalOffset, 0) × S(modelScale, modelScale, modelScale)
```

Equivalently at the graph field level:

```text
entityRootTranslation = (0, 24 + verticalOffset, 0)
entityRootRotation    = identity
entityRootScale       = (modelScale, modelScale, modelScale)
```

This ordering is intentional: `verticalOffset` is a CPM/player-space anchor translation and is **not itself multiplied by `modelScale`**; source descendants are scaled around the translated entity anchor.

`modelScale` must be finite and `>= 0.01`. `verticalOffset` must be finite. CPM 0.6.27 clamps render scale components to at least `0.01`, so any smaller positive scale would render differently from the requested transform. Projection must reject it with a diagnostic instead of relying on that silent runtime clamp. Mapping validation should be aligned to the same lower bound when the projector implementation is introduced.

The 24-pixel convention must be proven by world-transform golden tests. A source root pivot `(0,24,0)` with identity rotation and unit scale must compose under the BODY anchor to the expected neutral player-space reference.

Uniform `modelScale` belongs on `entity_root` because the observed CPM renderer applies element render scale to the matrix before descending into children. The scale therefore affects the whole entity subtree exactly once instead of resizing an individual cube.

T501 remains responsible for broader ground/scale calibration and visual acceptance. T300 establishes the single explicit boundary where scale and vertical offset act.

## Bone projection

For each `BoneIR`, create exactly one structural `CpmElementV1` of kind `BONE`.

Rules:

- editor-readable name comes from `sourceName`;
- key is `BONE:<BoneId>`;
- local transform is `bindLocal`, already in CPM-oriented `IR_LOCAL` coordinates;
- the bone itself has no visible geometry and uses the structural visibility rules above;
- source bone ancestry is preserved;
- no bone is partitioned into HEAD/arm/leg roots in T300.

Within each structural bone, child ordering is normative:

1. cube entries owned by that bone, in `BoneIR.cubes()` source order; each entry is either the cube itself or its helper subtree;
2. structural child bones in `BoneIR.children()` source order.

Cubes and child bones are siblings where appropriate, so this ordering does not change transforms; it only fixes deterministic graph/ID traversal behavior for T301.

A deep hierarchy such as `body -> spine -> chest -> neck -> head -> jaw/accessory` must remain the same logical ancestry below `entity_root`.

## Cube projection

A `CubeIR` is projected as a renderable CPM element owned by its declared bone.

For a cube with zero local pivot and identity rotation, the visible cube attaches directly below the structural bone. It preserves:

- size;
- parser-normalized local offset/origin semantics;
- inflate as CPM `mcScale`;
- mirror;
- box UV or per-face UV;
- texture/grid metadata.

Its node transform is identity relative to the owning bone.

For a cube with non-zero local pivot and/or non-identity rotation, create a geometry-free `CUBE_HELPER` below the structural bone. The helper owns the cube pivot/rotation transform and follows the structural visibility rules; the visible cube becomes its child with rotation neutralized. This prevents flattening two Euler transforms by component-wise addition.

The helper key is `CUBE_HELPER:<CubeId>` and the visible cube key is `CUBE:<CubeId>`.

No helper is created when it is mathematically unnecessary.

The helper path must preserve the same world-space cube transform that the unprojected `CubeIR` describes. This matches the pinned CPM Blockbench converter's use of generated rotation groups for rotated cubes.

## UV representability boundary

T300 keeps UV in the precision/orientation already preserved by `ModelIR`; it performs **no rounding, integer cast, absolute-value normalization or texture rewrite**.

This is required because Gecko 4.4.9 accepts `double` UV coordinates and signed per-face `uv_size`, while CPM project V1 stores the corresponding editor/project fields as integers. The official CPM Blockbench converter can compensate for fractional coordinates by upscaling/rebuilding texture space, but FR-004 deliberately forbids PNG re-encoding in the MVP.

Therefore:

- integral UV that maps exactly to CPM V1 remains serializable normally;
- fractional/signed UV remains losslessly represented in the T300 graph;
- T302/T303 must test exact CPM V1 representability before serialization and emit a stable error if exact output would require quantization or texture rewriting;
- no stage may silently truncate a Gecko UV merely to make the CPM JSON schema accept it.

## Source-feature precondition

T300 consumes a validated `ModelIR`; it must not try to reconstruct Gecko-only metadata that the adapter discarded.

The compatibility review found a pre-existing NFR-007 gap in the geometry parser. Pinned GeckoLib 4.4.9 raw `Bone` recognizes:

- `bind_pose_rotation`;
- `debug`;
- `inflate`;
- `locators`;
- bone `mirror`;
- `neverRender`;
- `render_group_id`;
- `reset`;
- `texture_meshes`;
- plus the already handled name/parent/pivot/rotation/cubes/poly_mesh fields.

The current parser handles the geometry necessary for the fixtures but does not yet preserve or explicitly reject every recognized item above. T205 is therefore a prerequisite hardening task: each recognized source feature must either be represented with demonstrated semantics or rejected/diagnosed according to the MVP subset. No feature recognized by the pinned Gecko parser may disappear silently.

`neverRender` deserves particular care because Gecko initializes the bone as hidden when it is true; it cannot simply be ignored or mapped to the CPM structural `hidden` flag because CPM hidden-parent recursion has different descendant behavior. T205 must establish its exact supported/rejected semantics before projection.

## Transform and rebake policy

The default single-anchor bone path does not reparent Gecko bones relative to each other. Any operation that does change parentage or absorbs transforms must preserve world transform using the accepted formula:

```text
newLocal = inverse(newParentWorld) × originalWorld
```

The resulting matrix may become a CPM transform only if `decomposeTrs(tolerance)` succeeds.

Non-representable cases are never approximated silently. Shear, singular scale, non-affine matrices and other non-TRS results produce a stable projection error and no successful graph.

T300 reuses `converter-core` matrix/quaternion/Euler utilities. It must not introduce a second transform implementation in `writer-cpm`.

The implementation plan must use the existing static transform tolerance where available; if none exists, it introduces one named projection tolerance with tests at both sides of the threshold.

## Rotation representation at the CPM boundary

`ModelIR` bind rotations are quaternions; CPM V1 static element rotations are Euler ZYX degrees. T300 performs a deterministic static boundary conversion.

For static bind elements without authored continuity history, use `EulerAnglesZYX.fromQuaternion(...)` and a deterministic equivalent branch. Do not route later animation samples through this static conversion; T400/T500 use `RotationContinuity` with source/sample hints.

The T300 graph stores the CPM-facing static Euler value so T302 serializes it directly rather than decomposing the quaternion again and potentially choosing another branch.

If the existing math layer reports a gimbal ambiguity that cannot be resolved deterministically under the static contract, projection emits a diagnostic rather than selecting an unstable arbitrary branch.

## Determinism

Identical `ModelIR` + settings must produce a logically identical ordered graph.

Required order:

- roots: HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG;
- Gecko roots under `entity_root`: `ModelIR.roots()` order;
- for each bone: cube/helper entries first in source cube order, then child bones in source child order;
- helper owns only its corresponding cube;
- logical target lookup is immutable and has deterministic iteration.

No UUID or random value is permitted. Final numeric IDs remain T301.

## Diagnostics and failure behavior

Projection returns the project-standard `Result<T>` and, when integrated into the complete pipeline, preserves prior diagnostics.

T300 requires stable errors for:

- non-representable TRS/shear during required rebake/helper projection;
- singular transform during inverse/reparent;
- missing referenced bone or cube ownership mismatch that reaches projection despite IR validation;
- non-finite projection settings;
- `modelScale < 0.01`.

Reuse an existing diagnostic code only when its meaning matches exactly; otherwise add projection-specific codes to `DiagnosticCodes` and document them in `specs/001-geckolib4-to-cpm/diagnostics.md`.

No partial CPM graph is returned as success after any projection error.

## Testing strategy

Implementation follows TDD. Required RED/GREEN coverage before T300 can be marked complete:

1. **single-anchor topology** — six roots exist; only BODY owns one `entity_root`; every Gecko root is below it;
2. **24-pixel root boundary** — source root pivot `(0,24,0)` composes to the expected neutral BODY/player reference;
3. **modelScale inheritance** — uniform entity-root scale changes descendant world points exactly once and never mutates `ModelIR`;
4. **modelScale CPM floor** — `0.01` is accepted exactly and any smaller positive value is rejected instead of being runtime-clamped;
5. **verticalOffset isolation/order** — offset changes only entity-root translation, is not multiplied by scale, and shifts all descendants in CPM/player space;
6. **structural visibility** — zero-size entity/bone/helper nodes remain `show=true`, `hidden=false`, `texture=false` so descendants stay renderable;
7. **stable bone targets** — bones with 0/1/many cubes still produce exactly one `BONE:<id>`;
8. **deep hierarchy** — body/spine/chest/neck/head/jaw/accessory ancestry is preserved;
9. **cube helper** — rotated/pivoted cube creates one helper, neutralizes the visible cube rotation and preserves world transform;
10. **no unnecessary helper** — neutral cube attaches directly to the owning bone;
11. **world-preserving rebake** — any explicit reparent path matches `inverse(parentWorld) × originalWorld`;
12. **shear rejection** — non-TRS rebake/helper result fails with a projection diagnostic;
13. **UV preservation** — fractional and signed source UV survives T300 unchanged; no integer quantization occurs in projection;
14. **settings validation** — non-finite offsets/scales and scale below the CPM exact floor fail deterministically;
15. **ordering/determinism** — repeated projection returns equal ordered topology and stable logical keys;
16. **immutability** — projection does not mutate `ModelIR` or caller settings.

Golden expected transforms are hand-derived from documented coordinate equations, never generated by the production projector under test.

Before these tests begin, T205 must prove that the source parser does not silently discard recognized Gecko bone/model features relevant to the supported subset.

After unit GREEN, the full repository gate is mandatory on the same final commit: Spotless, `clean check`, reproducibility, fixture manifest and S004 oracle under the established Ubuntu/Windows CI workflow.

## Scope exclusions

T300 does not implement:

- Gecko source-feature subset hardening — T205 prerequisite;
- final numeric `storeID` allocation — T301;
- JSON/ZIP/PNG output and UV serializability enforcement — T302;
- independent CPM artifact validation — T303;
- ProjectIO/editor static acceptance — T304;
- animation sampling/projection — T400–T403;
- dynamic head/neck look composition — T500;
- final ground calibration — T501;
- CLI/report orchestration — T600+.

If T300 implementation reveals a requirement that cannot be represented without changing single-anchor topology, the design/ADR is reopened instead of silently introducing root partitioning.

## Evidence and upstream references

This design relies on the existing project evidence and pinned upstream behavior:

- `docs/decisions/ADR-003-cpm-output-strategy.md` — independent CPM writer;
- `docs/decisions/ADR-005-head-retargeting.md` — provisional single-anchor architecture;
- `docs/coordinate-systems.md` — coordinate boundary and world-preserving reparent formula;
- `docs/cpm-project-format.md` — CPM V1 roots/elements/storeID contract;
- `specs/001-geckolib4-to-cpm/data-model.md` — separation of `ModelIR` and CPM projection graph;
- `spikes/head-layering/results.md` — automatic single-anchor inheritance/layering evidence;
- CPM commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`, especially `ElementsLoaderV1`, `ElementType`, `Cube`, `RenderedCube`, `RootModelElement`, `ModelRenderManager`, `PerFaceUV`, `PlayerPartValues` and `BlockbenchExport`;
- GeckoLib commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`, especially raw `Bone`, raw `Cube`, `BakedModelFactory` and `GeoBone`.

The visual acceptance caveat from ADR-005 remains unchanged: T300 may implement this architecture after the parser hardening gate, but the project must not claim final visual head/look acceptance until S001/S002/T701 are completed.
