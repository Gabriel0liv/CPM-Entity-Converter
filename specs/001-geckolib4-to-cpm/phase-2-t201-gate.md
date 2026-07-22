# Phase 2 T201 Gate

Date: 2026-07-22
Commit base: b18ec22cdca346052a980e4b6c5a02b04a0a5d65
Implementation HEAD reviewed: ad85b0802b96a592949ae37d8f1dac37a8f57d5f
Independent review: review/t201-final-acceptance (technical review pending)
Workflow run: 29933174675
Workflow HEAD: ad85b0802b96a592949ae37d8f1dac37a8f57d5f
Ubuntu: PASS
Windows: PASS

## Evidence

The integrated implementation contains the UV/PNG boundary, canonical
per-face ordering, Gecko box-layout derivation, logical PNG source paths, and
fixture-only static assembly smoke coverage. Local `clean check`, reproducible
build, manifest check, S004 audit and GeckoLib oracle check pass. The oracle
regression reports 41/41 assertions passed with clips A `idle,walk`, B
`idle,walk`, C `idle`, and D `walk`.

## Frozen acceptance checklist

UV diagnostics and `Result` error semantics: PASS for the covered decoder paths.
Per-face order and defensive copy: PASS.
Box layout formulas and floor semantics: PASS in `GeckoBoxUvLayoutTest`.
Signed/fractional UV transport: PASS in core/decoder coverage.
PNG validation, logical paths and local limits: PARTIAL; a complete matrix of
invalid files, all limits and pointer/context assertions is not yet present.
Dimension policy: PASS in `GeckoStaticModelAssembler`.
Bounds: PARTIAL; implementation evaluates box faces, but the complete frozen
edge/inversion matrix is not yet represented by tests.
Static ModelIR A-D: PARTIAL; `StaticModelFixtureTest` executes all four fixtures
and confirms clips remain empty, but canonical `model-static.json` snapshots are
not yet versioned and compared.
ModelIrValidator static invariants: PARTIAL; assembler invokes the validator,
but the complete T201-specific invariant matrix is pending.
Fixture geometry snapshots: PASS for the existing geometry-only A-D snapshots.
Manifest: PASS (`manifest.py --check`).
Oracle regression: PASS (41/41).

T201 decision: **[~] partial**. CI is green, but the technical gate remains
closed until the concrete PNG/limits, bounds, and static ModelIR golden tests
above are added and independently reviewed.

Deferred to T202: animation clips, tracks, keyframes and playback.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile/fuzz matrix and differential oracle.
Deferred to T300/T700: CPM projection, helper nodes, output and broad filesystem
compatibility.
