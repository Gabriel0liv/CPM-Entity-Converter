# Phase 2 T200 Gate

Date: 2026-07-22
Commit base: 22d954abbb5dc616010b500968127fbcbff6f16b
Implementation HEAD: acc9f61 (integration/phase2)
Independent review: review/t200-final-pass
Workflow: 29917081549 (`acc9f61859210ade273894c5949ddd5be527f5b4`)
Ubuntu: PASS
Windows: PASS

## Scope evidence

T200 implements the 1.12.0 geometry selection boundary, static bones, local bind
transforms, cubes, source order, provenance pointers, raw UV transport and
parser-local limits. `ParsedGeometry` is the explicit boundary to T201; UV/PNG
resolution, animation parsing, easing, sampling, CPM projection and writing are
deferred.

Observed upstream evidence includes `MinecraftGeometry`/`BakedModelFactory`
semantics at GeckoLib commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.

## Verification

`clean check`, adapter tests, reproducible build, fixture manifest and the S004
audit pass locally. The A–D fixture oracle was regenerated against the pinned
GeckoLib checkout and now reports 41/41 assertions passed; clips remain
A `idle/walk`, B `idle/walk`, C `idle`, D `walk`.

Independent review classification: **PARTIAL**. The validator is now invoked by
the parser and covers graph, roots, ownership, finiteness, provenance and
cycles; geometry goldens compare identifiers, topology, translations and cube
counts. The remaining review concern is that the complete hostile/feature/path
matrix and all cube-level golden fields are not yet independently asserted.
Therefore T200 is not marked complete and the phase-2 gate remains closed.

FR-001: partial parse evidence
FR-002: partial static geometry evidence
FR-006: partial geometry selection/graph evidence
FR-007: partial bind conversion evidence
FR-009: raw cube boundary evidence
NFR-012: parser-local dependency boundary verified
CON-001: upstream checkouts unchanged

Deferred to T201: UV/PNG validation and static ModelIR assembly.
Deferred to T202: animation clips, tracks and keyframes.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile inputs, full limits and oracle comparison matrix.

T200 decision: **[~] partial; corrective evidence remains**. CI is green on the
integrated corrective HEAD, but the complete cube-level snapshot schema and
the full table-driven path/value/feature test matrix are not yet present.
