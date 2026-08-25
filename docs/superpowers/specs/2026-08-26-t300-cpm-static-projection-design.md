# T300 — Static CPM projection design

Status: **approved in chat; written-spec review pending before implementation plan**.

Date: 2026-08-26.
Branch: `agent/correct-look-retargeting-phase1`.

## Goal

T300 creates the first production boundary from `ModelIR` to a CPM-specific static project graph. The output of this task is **not yet a `.cpmproject` ZIP** and does not assign final CPM `storeID` values. It creates a deterministic, writer-owned representation of CPM roots/elements/helper nodes that preserves the source hierarchy, bind transforms, cube ownership, cube pivots/rotations and global projection settings.

The task implements FR-011/FR-012 and prepares FR-007/FR-010/FR-026. It must not pull animation, look-retargeting, ZIP serialization, final numeric IDs or CLI concerns forward from T301/T302/T400/T500.

Correctness remains the three-level contract already defined by the project: structural correctness is required here; animation and semantic correctness must remain possible without changing the topology introduced by T300.

## Chosen architecture

Use **single-anchor projection with stable structural bone nodes**.

```text
CPM HEAD       hidden vanilla root
CPM BODY       hidden vanilla root
└── entity_root
    ├── gecko_root_A
    │   ├── bone...
    │   │   ├── cube
    │   │   └── cube_pivot_helper
    │   │       └── cube
    │   └── ...
    └── gecko_root_B...
CPM LEFT_ARM   hidden vanilla root
CPM RIGHT_ARM  hidden vanilla root
CPM LEFT_LEG   hidden vanilla root
CPM RIGHT_LEG  hidden vanilla root
```

Every Gecko bone becomes a CPM structural element even when the bone has zero or one cube. This is deliberate. A logical bone such as `body`, `neck`, `head`, `jaw` or an accessory must have a stable target independent of cube count, so later animation and look projection never need to retarget because geometry changed.

This differs from compacting a one-cube bone directly into the cube element. The extra nodes are accepted because they preserve identity, hierarchy and editability while minimizing later retarget complexity.

## Module boundary

`writer-cpm` remains dependent only on `converter-core`.

T300 must **not** add a dependency from `writer-cpm` to `converter-config`. Instead it introduces a small projection input owned by `writer-cpm`, conceptually:

```text
CpmProjectionSettings
  modelScale: double
  verticalOffset: double
  anchor: BODY                 # MVP fixed by accepted single-anchor strategy
  hideVanillaRoots: boolean    # production default true for replacement model
  disableVanillaAnimation: boolean/policy placeholder
```

The later orchestration layer translates `SemanticRigMap` into this settings object. This keeps the CPM projection reusable and prevents config-schema classes from leaking into the writer layer.

The exact public API may be records/classes following existing Java style, but the semantic boundary above is normative.

## CPM project graph owned by `writer-cpm`

T300 introduces a CPM-specific graph separate from `ModelIR`. It must carry enough information for T301/T302 without prematurely serializing JSON.

Conceptual model:

```text
CpmStaticProjectV1
  roots: ordered list<CpmRootV1>
  texture metadata/reference
  logicalTargets: map<ProjectionKey, node>

CpmRootV1
  vanillaPart: HEAD | BODY | LEFT_ARM | RIGHT_ARM | LEFT_LEG | RIGHT_LEG
  visible: boolean
  disableVanillaAnim: boolean
  transform
  children

CpmElementV1
  key: ProjectionKey
  name
  kind: ENTITY_ROOT | BONE | CUBE_HELPER | CUBE
  transform
  geometry/appearance when kind == CUBE
  children
```

`ProjectionKey` is a deterministic logical identity, not a final numeric CPM ID. Required key domains:

- `ENTITY_ROOT`
- `BONE:<BoneId>`
- `CUBE_HELPER:<CubeId>`
- `CUBE:<CubeId>`

T301 consumes this graph and assigns numeric `storeID` values in canonical preorder. T300 must therefore preserve deterministic child ordering and expose all nodes that later animations may reference.

## Vanilla roots

CPM creates the six vanilla player roots independently of custom children. A converted entity is intended to replace the player model, not render on top of Steve/Alex, so the static graph explicitly represents all six roots and defaults them to hidden for production conversion.

Only BODY receives `entity_root` in the single-anchor architecture. HEAD/arms/legs remain present as CPM roots because they are part of the V1 project contract, but they do not receive converted Gecko subtrees.

T300 represents `disableVanillaAnim` explicitly on the root graph but does **not** hard-code the final policy decision. S001/S002 proved single-anchor hierarchy/layer ordering automatically but did not complete the visual camera/editor gate. T400/T500/T701 may choose the final value/policy after combined vanilla + converted animation behavior is observable.

Static visibility and vanilla-animation inheritance are therefore separate concerns.

## `entity_root` and global projection boundary

`entity_root` is synthetic, geometry-free and exists only to isolate global CPM projection from the original Gecko hierarchy.

The parser currently stores root-bone bind translation as:

```text
C(rootPivot) = (-x, -y, +z)
```

while the observed CPM/Blockbench root conversion uses the player-space Y baseline of 24 pixels. Under the BODY single-anchor strategy, `entity_root` owns this root-space offset rather than modifying every Gecko root.

Normative baseline:

```text
entityRootTranslation = (0, 24 + verticalOffset, 0)
entityRootRotation    = identity
entityRootScale       = (modelScale, modelScale, modelScale)
```

Default `modelScale` is `1`. Default `verticalOffset` is `0`.

The implementation must verify this convention with golden transforms rather than merely asserting field values. A source root pivot `(0, 24, 0)` with identity transform must land at the expected BODY/player reference after `entity_root × sourceRootLocal` composition.

The scale belongs on `entity_root` because the observed CPM renderer applies element render scale to the matrix before descending into children. Therefore uniform model scale is inherited by the whole entity subtree instead of resizing only one cube.

T501 remains responsible for broader ground/scale calibration and visual acceptance; T300 establishes the single, explicit boundary where those settings act.

## Bone projection

For each `BoneIR`, create exactly one structural `CpmElementV1` of kind `BONE`.

Rules:

- name comes from `sourceName` for editor readability;
- key is `BONE:<BoneId>`;
- local transform is the bone bind local transform already expressed in CPM-oriented `IR_LOCAL` coordinates;
- children preserve source hierarchy order;
- cube children appear at the bone before child bones only if the chosen graph ordering contract says so consistently; the implementation plan must choose one deterministic ordering and test it;
- the bone element itself contains no visible geometry.

The topology of Gecko bones below `entity_root` is unchanged unless an explicit world-preserving helper/rebake is necessary. T300 does not partition bones among vanilla CPM roots.

Deep hierarchy such as `body -> spine -> chest -> neck -> head -> jaw/accessory` must remain structurally identical below `entity_root`.

## Cube projection

A `CubeIR` is projected as a renderable CPM element owned by its declared bone.

For a cube whose local pivot is zero and whose rotation is identity, the cube can be attached directly below the structural bone. Its CPM cube fields preserve:

- size;
- local offset/origin semantics already normalized by the parser;
- inflate -> `mcScale`;
- mirror;
- box UV or per-face UV;
- texture usage/grid metadata.

The cube element's transform is identity relative to the bone unless its `CubeIR` local transform requires otherwise.

For a cube with non-zero local pivot and/or non-identity rotation, create a geometry-free `CUBE_HELPER` child below the bone. The helper owns the cube pivot/rotation transform. The visible cube becomes a child of that helper with its own rotation neutralized. This prevents flattening two Euler transforms by component-wise addition.

The helper key is `CUBE_HELPER:<CubeId>` and the visible child key is `CUBE:<CubeId>`.

No helper is created when it is mathematically unnecessary.

## Transform and rebake policy

The default single-anchor path should not need hierarchy rebake for normal bones. Any operation that changes parentage or absorbs transforms must preserve world transform using the already accepted formula:

```text
newLocal = inverse(newParentWorld) × originalWorld
```

The resulting matrix may only become a CPM element transform if `decomposeTrs(tolerance)` succeeds.

Non-representable cases are not approximated. Shear, singular scale, non-affine matrices or other unsupported transforms produce a stable error diagnostic and no successful projection result.

T300 should reuse the math layer in `converter-core`; it must not introduce a second matrix/Euler implementation inside `writer-cpm`.

The default decomposition tolerance should use the project's established static transform tolerance unless the implementation plan identifies a single existing constant to reuse.

## Rotation representation at the CPM boundary

`ModelIR` bind rotations are quaternions. CPM V1 elements serialize Euler ZYX degrees. T300 therefore needs a deterministic static conversion at the graph boundary.

For static bind elements without an authored continuity history, use `EulerAnglesZYX.fromQuaternion(...)` and a deterministic equivalent branch. Do not normalize later animation samples through this static path; T400/T500 use `RotationContinuity` with authored/sample hints.

The static graph stores the final CPM-facing Euler representation or an equivalent writer-specific rotation value that T302 can serialize without recomputing a different branch.

Gimbal ambiguity that cannot be represented deterministically under the existing math contract must become a diagnostic rather than an arbitrary unstable choice.

## Determinism

T300 output must be deterministic for identical `ModelIR` + settings.

Required ordering:

- vanilla roots in fixed CPM order: HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG;
- Gecko roots under `entity_root` in `ModelIR.roots()` order;
- bone children in `BoneIR.children()` order;
- cubes in `BoneIR.cubes()` source order;
- helper immediately owns only its corresponding cube;
- logical key lookup is exposed through immutable deterministic views, never observable `HashMap` order.

No UUID/random ID is permitted. Final numeric IDs remain T301.

## Diagnostics and failure behavior

Projection returns the project-standard `Result<T>` and preserves incoming diagnostics when integrated into the full pipeline.

T300 needs stable error codes for at least:

- non-representable TRS/shear during required rebake;
- singular transform during inverse/reparent;
- missing referenced bone/cube ownership inconsistency reaching projection despite prior validation;
- non-finite projection settings;
- invalid/non-positive `modelScale` if the selected CPM contract forbids it.

The implementation plan should reuse an existing diagnostic code where its meaning exactly matches; otherwise add projection-specific codes to `DiagnosticCodes` and document them in `specs/001-geckolib4-to-cpm/diagnostics.md`.

No partial CPM graph is returned as success after a projection error.

## Testing strategy

Implementation follows TDD. Required RED/GREEN coverage before T300 can be marked complete:

1. **single-anchor topology** — six roots exist, only BODY owns one synthetic `entity_root`, and all Gecko roots are below it;
2. **24-pixel root boundary** — source root pivot `(0,24,0)` produces the expected composed BODY-space neutral transform;
3. **modelScale inheritance** — entity-root uniform scale changes child world points exactly once and does not mutate source `ModelIR`;
4. **verticalOffset isolation** — offset changes only `entity_root` and shifts every descendant equivalently;
5. **stable bone targets** — 0/1/many-cube bones still produce exactly one `BONE:<id>` node;
6. **deep hierarchy** — body/spine/chest/neck/head/jaw/accessory ancestry is preserved;
7. **cube helper** — rotated/pivoted cube creates helper, neutralizes visible cube rotation and preserves world-space cube transform;
8. **no unnecessary helper** — neutral cube attaches directly to bone;
9. **world-preserving reparent math** — any explicit rebake path matches `inverse(parentWorld) × originalWorld`;
10. **shear rejection** — non-TRS rebake fails with a projection diagnostic;
11. **determinism** — repeated projection produces logically equal ordered graph and stable logical keys;
12. **immutability** — projection does not mutate `ModelIR` or caller settings.

Golden expected transforms must be hand-derived from the documented coordinate equations, not generated by the production projector under test.

After unit GREEN, the full repository gate remains mandatory: Spotless, `clean check`, reproducibility, fixture manifest and S004 oracle on the same final commit for Ubuntu/Windows according to the established CI workflow.

## Scope exclusions

T300 explicitly does not implement:

- final numeric `storeID` allocation — T301;
- JSON/ZIP/PNG output — T302;
- independent CPM artifact validation — T303;
- ProjectIO/editor static acceptance — T304;
- animation sampling/projection — T400–T403;
- dynamic head/neck look composition — T500;
- final ground calibration — T501;
- CLI/report orchestration — T600+.

If implementing T300 reveals a requirement that cannot be represented without changing the single-anchor topology, stop and re-open the design/ADR instead of silently introducing root partitioning.

## Evidence and upstream references

This design relies on the existing project evidence and pinned upstream CPM behavior:

- `docs/decisions/ADR-003-cpm-output-strategy.md` — independent CPM writer;
- `docs/decisions/ADR-005-head-retargeting.md` — provisional single-anchor architecture;
- `docs/coordinate-systems.md` — coordinate boundary and world-preserving reparent formula;
- `docs/cpm-project-format.md` — CPM V1 roots/elements/storeID contract;
- `specs/001-geckolib4-to-cpm/data-model.md` — separation of `ModelIR` and CPM projection graph;
- `spikes/head-layering/results.md` — automatic single-anchor inheritance/layering evidence;
- CPM commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`, especially `ElementsLoaderV1`, `ElementType`, `RenderedCube`, `ModelRenderManager` and `BlockbenchExport`.

The visual acceptance caveat from ADR-005 remains unchanged: T300 may implement this architecture before the graphical gate, but the project must not claim final visual head/look acceptance until S001/S002/T701 are completed.
