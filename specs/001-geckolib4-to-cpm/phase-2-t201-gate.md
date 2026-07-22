# Phase 2 T201 Gate

Date: 2026-07-22
Commit base: b18ec22cdca346052a980e4b6c5a02b04a0a5d65
Implementation HEAD reviewed: 54ee852
Independent review: review/t201-final-closure — FAIL
Workflow run: pending for 54ee852
Workflow HEAD: 54ee852
Ubuntu: PENDING
Windows: PENDING

## Evidence executed

The adapter now emits logical PNG pointers (`/`, `/signature`, `/IHDR`,
`/IHDR/width`, `/IHDR/height`, `/pixels`, `/decode`) and structured limit
contexts. UV malformed-field diagnostics use field/component pointers. Tests
cover valid PNG bytes/hash preservation, logical paths, signature failure and
the four local PNG limits. Box layout and per-face canonical ordering remain
covered. Local adapter tests pass; the previous integrated workflow reported
41/41 GeckoLib oracle assertions and the current oracle/manifest checks pass.

## Frozen checklist status

- PNG success/failure matrix: PARTIAL; the full invalid-file matrix is not yet
  represented.
- Four PNG limits and pointer/context checks: PASS for maxBytes/maxWidth/
  maxHeight/maxPixels tests.
- Box/per-face UV and bounds matrix: PARTIAL; canonical layout exists, but the
  complete malformed, orientation and bounds table is still incomplete.
- Logical path smoke: PARTIAL; core logical-source cases are covered, broad
  filesystem matrix remains deferred.
- Static ModelIR and invariants: PARTIAL; A-D smoke assembly confirms success and
  empty clips, but no integral `model-static.json` snapshots are versioned and
  compared, and the full relational invariant matrix is pending.
- Manifest: PASS.
- Oracle: PASS, 41/41 assertions on the pinned GeckoLib checkout.
- Clean check/reproducibility: PASS locally.

T201 decision: **[~] partial**. Technical review remains FAIL because the
frozen checklist still lacks the integral static snapshots and complete UV/PNG
evidence. T201 is not released.

Deferred to T202: animation clips, tracks, keyframes and playback.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile/fuzz matrix and differential oracle.
Deferred to T300/T700: CPM projection, output and broad filesystem matrix.
