# T304 ProjectIO Static Conformance Design

**Status:** proposed design for T304 on `agent/correct-look-retargeting-phase1`.

**Goal:** Prove that the current converter's static CPM V1 artifacts are accepted and materialized correctly by the official CPM 0.6.27 `ProjectIO`, without introducing CPM/Minecraft runtime dependencies into production modules, while keeping visual acceptance explicitly human and separate.

**Pinned CPM reference:** commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`.

**Primary acceptance criteria:** `AC-001` through `AC-005`, plus the static portions of `AC-020`, `AC-022`, and `AC-028`. Animation projection, look retargeting, 100-loop behavior, and CLI publication remain outside T304.

## Context and constraints

The current branch already owns the production static pipeline in `writer-cpm`: `CpmStaticProjector`, `CpmStoreIdAllocator`, and `CpmProjectWriterV1`. `validator-cpm` independently validates existing and converter-generated CPM V1 archives. T304 must verify the bytes emitted by those current production APIs against the official CPM loader; it must not reconstruct an alternate artifact format inside the verification harness.

The historical branch `feature/t304-projectio-static-conformance` is evidence and reference only. It is strongly diverged from the current branch, so it will not be merged wholesale. Useful ideas from it—headless `ProjectIO` loading, temporary save/reopen, stable reports, and a manual evidence bundle—will be reimplemented against the current APIs.

Production modules must continue to have no dependency on CPM, Minecraft, Forge, GeckoLib runtime classes, or the CPM editor. CPM source is compiled only inside the verification module from a separately checked-out, commit-pinned reference tree.

## Approaches considered

### A. Dedicated verification-only module with pinned CPM source — selected

Add a root Gradle module named `verification-projectio`. It depends on the current converter modules and compiles the selected CPM shared/editor source tree only when `-PcpmReferenceDir=<path>` is supplied. CI checks out the pinned CPM commit in a separate directory and runs this module as an explicit gate.

Advantages: preserves the production dependency boundary, tests the actual current writer output, supports direct ProjectIO introspection, and keeps heavy upstream compilation out of ordinary unit tests. It also makes the external evidence auditable and replaceable when the CPM pin changes.

### B. Put ProjectIO tests inside `validator-cpm` — rejected

This would couple the independent validator to CPM implementation details and would blur the distinction between validating the documented V1 contract and verifying one pinned loader implementation. It would also make the normal validator test graph heavier and risk accidental upstream dependencies leaking into production-facing modules.

### C. Merge or cherry-pick the old T304 branch wholesale — rejected

The old branch is more than two hundred commits divergent from the current branch and was built around older projection/writer APIs. Reusing it wholesale would reintroduce superseded architecture. Individual ideas and test assertions may be ported only after being checked against the present code.

## Module boundary

Create `verification-projectio/` with one responsibility: execute official CPM 0.6.27 ProjectIO conformance against artifacts produced by the current converter.

It will contain:

- `CurrentFixturePipeline`: builds fixture A/B/C/D from the existing fixture inputs using the current production parser/config/projector/store-ID/writer path; it returns the exact `.cpmproject` bytes emitted by `CpmProjectWriterV1`.
- `ProjectIoHarness`: initializes the pinned CPM editor classes headlessly, loads an archive through official `ProjectIO`, extracts a stable structural snapshot, and performs save/reopen through a temporary file.
- `ProjectIoSnapshot`: converter-owned immutable data used by assertions so tests do not spread reflection or CPM implementation details.
- JUnit tests grouped by contract: load/container, persisted IDs/references, texture/UV, hierarchy/bind transforms, round-trip, and cross-platform deterministic bytes.
- A small evidence runner that writes `build/t304/` reports and the manual-review bundle; generated build outputs are never committed.

The module is verification-only. No production module may depend on it.

## Current production pipeline used by T304

For each fixture A/B/C/D, the harness uses the current project APIs rather than the old branch's writer stack:

1. Parse Gecko geometry and texture with the current `adapter-geckolib4` static path.
2. Load and compile the fixture mapping with `MappingLoader` and `MappingCompiler`; the resulting `SemanticRigMap` supplies the configured `modelScale` and `verticalOffset`.
3. Create `CpmProjectionSettings` from the compiled static configuration. For this T304 static gate, `hideVanillaRoots=true` and `disableVanillaAnim=true` remain explicit harness inputs rather than inferred loader behavior.
4. Call `CpmStaticProjector.project(ModelIR, CpmProjectionSettings)`.
5. Call `CpmStoreIdAllocator.allocate(CpmStaticProjectV1)`.
6. Call `CpmProjectWriterV1.write(CpmStaticProjectV1, CpmStoreIdPlan)`.
7. Validate the resulting bytes with `CpmProjectValidator` under `GENERATED_V1` before invoking upstream `ProjectIO`. A validator failure is a converter failure and stops the ProjectIO stage.
8. Pass exactly those same bytes to the pinned ProjectIO harness. No ZIP, JSON, UV, ID, or texture rewriting is allowed in the verification layer.

Animations are deliberately not serialized by the current static writer and therefore are not invented by T304. Animation filename/state conformance remains for the later animation projection phase.

## ProjectIO observations and assertions

The harness must load the artifact using the official CPM project path, not by directly parsing `config.json` with converter code.

For A/B/C/D it records and asserts:

- load succeeds without an exception;
- six vanilla roots are materialized;
- the static hierarchy contains `BODY -> entity_root -> Gecko roots` for the single-anchor strategy;
- every persisted generated `storeID` from the converter artifact maps to exactly one loaded element and remains within the T301 safe range;
- texture presence, texture dimensions, box/per-face UV origins, and face orientation data are observable consistently with the artifact/fixture expectation;
- local position, rotation, and scale for loaded bind elements match the converter static graph within the existing tolerances: position `<= 1e-4` pixel, rotation `<= 1e-4°`, scale `<= 1e-6`;
- parent paths in the loaded ProjectIO editor match the converter static graph;
- save to a temporary `.cpmproject`, close, reopen, and re-observe the same structural invariants succeeds.

The round-trip comparison is semantic/structural, not byte equality. CPM's own save implementation is not required to preserve the converter's canonical ZIP/JSON bytes.

S003 artifacts M2/M3/M4/M5 are also loaded through the same pinned ProjectIO harness as a compatibility control. M0/M1 remain expected negative controls only if the established S003 oracle still classifies them as invalid.

## Determinism and cross-platform evidence

`AC-005` is proved on the converter output before ProjectIO touches it.

For each fixture A/B/C/D:

- generate twice in one process and require byte-identical output;
- compute SHA-256 of the emitted bytes;
- run the same generation in Ubuntu and Windows CI;
- compare each OS result with a committed expected manifest once the first current-architecture reference run is verified;
- fail if either OS produces a different hash.

The expected hashes from `feature/t304-projectio-static-conformance` are not reused automatically because that branch used a different writer/projection architecture. Current-branch hashes must be established from current output and then frozen.

## CI design

Keep the existing normal `check` matrix unchanged as the fast production gate. Add a separate `projectio-conformance` matrix job for Ubuntu and Windows:

1. checkout this repository;
2. setup Java 17;
3. checkout `tom5454/CustomPlayerModels` at exact commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` into a sibling or runner-temp directory;
4. verify `git rev-parse HEAD` equals the pin before compiling;
5. run `:verification-projectio:spotlessCheck` and the ProjectIO conformance tests with `-PcpmReferenceDir=<checkout>`;
6. run the evidence generator;
7. upload the T304 evidence bundle on success and on failure when available.

The normal root `check` must not require a local CPM checkout. `verification-projectio` therefore skips its CPM-dependent compile/test tasks when the property is absent, while the dedicated CI job treats the property as mandatory.

## Error boundaries

Failures are classified so the evidence identifies the broken layer:

- **converter pipeline failure:** geometry/config/projection/store-ID/writer returns a diagnostic failure;
- **generated-validator failure:** current output violates T303 `GENERATED_V1`;
- **CPM load failure:** official ProjectIO throws/rejects the archive;
- **materialization mismatch:** ProjectIO loads but IDs, UV, hierarchy, or bind values differ from expected static semantics;
- **round-trip failure:** official save/reopen cannot preserve the required static invariants;
- **determinism failure:** two current converter runs or two supported OSes emit different bytes.

Reports record fixture, converter commit, CPM commit/version, artifact SHA-256, stage, and stable observed values. Stack traces remain CI diagnostics, not committed golden content.

## Manual visual acceptance

Automation cannot complete the visual editor criteria. T304 therefore generates `build/t304/manual-evidence/` containing:

- the exact A/B/C/D `.cpmproject` artifacts that passed automation;
- artifact hashes and CPM pin;
- the ProjectIO structural report;
- a checklist for open, texture/UV, hierarchy/bind, and Save As/reopen;
- per-fixture screenshot directories.

The checklist starts as `NOT RUN`. No automation may mark visual rows PASS. T304 remains `[~]` until a human opens the exact hashed artifacts in CPM Editor 0.6.27 and records the required observations. The automated sub-gate may be marked PASS separately.

## Test strategy and TDD order

Implementation follows RED -> GREEN for each independent contract:

1. Module/pin gate: tests fail because ProjectIO harness/module does not exist, then compile the pinned source in isolation.
2. Current fixture pipeline: tests fail until A/B/C/D are generated by current production APIs and pass `GENERATED_V1`.
3. ProjectIO load/S003 controls: tests fail until official load is wired correctly.
4. IDs and hierarchy/bind: add failing snapshot assertions, then implement stable extraction.
5. Texture/UV: add failing observations for textured fixtures, then implement extraction/assertions.
6. Round-trip: add failing save/reopen assertions, then wire official save path using temporary files only.
7. Determinism: generate current hashes, verify both OSes, freeze the manifest, then rerun the cross-platform gate.
8. Evidence/manual bundle: verify automation never writes a false visual PASS.

The final automated T304 gate requires both Ubuntu and Windows ProjectIO jobs green plus the normal repository CI green.

## Non-goals

T304 does not implement or claim:

- animation sampling or CPM animation projection (`T400`–`T403`);
- head/neck look retargeting (`T500`);
- animation visual criteria `AC-021`, `AC-023`–`AC-027`, `AC-029`;
- CLI `convert/inspect/validate` behavior (`T600+`);
- successful visual acceptance without a human editor session.

## Completion rule

The automated portion of T304 is complete only when the current A/B/C/D artifacts pass the pinned ProjectIO load/materialization/round-trip and cross-platform deterministic-byte gates, S003 M2–M5 remain compatible controls, and the evidence bundle is generated from the same passing hashes.

The task itself is marked `[x]` only after the manual CPM Editor checklist for the exact passing A/B/C/D artifacts is completed. Until then `tasks.md` keeps T304 `[~]` with the automated run IDs and the visual status explicitly pending.
