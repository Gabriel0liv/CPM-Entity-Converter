# Phase 2 T201 Gate

Date: 2026-07-22
Commit base: 25f2a64022b78f6d50b220b5daa7b53908942d4b
Implementation commit: 03be749
Evidence test commit: 8484bc7
Independent review: not yet executed
Workflow: not yet dispatched

## Local evidence

`BoxUvIR`/`FaceUvIR` preserve finite doubles and signed dimensions;
`PerFaceUvIR` is immutable and canonicalized by `CubeFaceIR`. `PngTextureValidator`
validates PNG signature, IHDR dimensions, decodeability, byte size and local
limits without re-encoding. `GeckoUvDecoder` and
`GeckoStaticModelAssembler` are present, and adapter tests cover basic PNG byte
preservation and signed/fractional box UV.

## Frozen acceptance checklist

PNG validation: PARTIAL
PNG byte preservation: PASS (basic test)
PNG limits: PARTIAL
Dimension policy: PASS in assembler
Box UV: PARTIAL
Per-face UV: PARTIAL
Signed `uv_size`: supported by core types; integration tests pending
Orientation: NOT YET PROVEN
Mirror: preserved, orientation tests pending
Bounds: implemented in decoder; matrix pending
Static ModelIR: assembler present; A–D integration/goldens pending
ModelIrValidator: invoked by assembler; full static invariant coverage pending
Fixture goldens: NOT GENERATED
Manifest: unchanged for T201 outputs
Oracle: 41/41 regression remains available, not evidence of T201

T201 decision: **[~] partial; implementation and evidence remain incomplete**.

Deferred to T202: animation clips, tracks, keyframes and playback.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile/fuzz matrix and differential oracle.
Deferred to T300: CPM projection, helper nodes, anchors and output.
