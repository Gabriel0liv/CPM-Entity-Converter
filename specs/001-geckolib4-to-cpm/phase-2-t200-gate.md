# Phase 2 T200 Gate

Date: 2026-07-22
Commit base: 22d954abbb5dc616010b500968127fbcbff6f16b
Implementation HEAD: 22f5cc3 (integration/phase2)
Independent review: review/t200-geometry-parser
Workflow: not yet green for this HEAD
Ubuntu: pending
Windows: pending

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
audit pass locally. The fixture oracle check currently reports its existing
artifact as stale after the harness regenerates its manifest; this is retained
as a separate pre-existing spike issue and is not presented as T200 evidence.

Independent review classification: **PARTIAL**. Remaining concrete gaps are
the complete parsed-boundary validator matrix, full independent geometry golden
comparisons and the complete hostile/feature/limit test matrix. Therefore T200
is not marked complete and the phase-2 gate remains closed.

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

T200 decision: **[!] remains blocked pending the listed corrective evidence**.
