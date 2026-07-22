# Phase 2 T201 Gate

Date: 2026-07-22
Commit base: ee3cc5c2e5b408297c59776820043e6bc6c4a274
Intermediate workflow: 29944929015 (ee3cc5c; Ubuntu PASS; Windows PASS)
Implementation/evidence HEAD: bb60053
Independent review: review/t201-final-acceptance-v2 — pending
Workflow run: pending for bb60053
Workflow HEAD: bb60053
Ubuntu: PENDING
Windows: PENDING

## Evidence completed in P2.10

- PNG logical diagnostics and the four local limits are covered with pointers,
  limitName, limit, observed and logical sources.
- Box and per-face UV tests preserve signed/fractional values and canonical
  face order; the existing Gecko box formulas and bounds behavior remain tested.
- `StaticModelSnapshot` is test-only and compares the complete static ModelIR
  tree for fixtures A–D. The four reviewed `expected/model-static.json` files
  include source, geometry, roots, textures, empty clips, bones, bind transforms,
  provenance, cubes and box/per-face UV data.
- Static assembly tests confirm one texture, empty clips, validator execution and
  no animation input. PNG bytes remain unchanged. Manifest and GeckoLib oracle
  regression pass; oracle reports 41/41 assertions.
- Local `clean check`, reproducible build, manifest check and S004 audit pass.

## Frozen checklist

PNG matrix: PASS for the implemented local scope.
PNG limits: PASS (`maxBytes`, `maxWidth`, `maxHeight`, `maxPixels`).
PNG pointers/context: PASS.
Box UV matrix: PASS.
Per-face UV matrix: PASS for signed/fractional/order/material warning paths.
Bounds: PASS for the implemented box/per-face boundary behavior.
UV diagnostics: PASS for covered malformed, missing and unknown-face paths.
Logical path smoke: PASS for the T201 boundary scope.
Record invariants: PASS through constructors and tests.
Static ModelIrValidator/assembler: PASS for represented relational invariants.
Snapshots A–D: PASS; expected trees compare integrally.
PNG bytes: PASS.
Manifest: PASS.
Oracle: PASS, 41/41.
Clean check: PASS locally.

T201 decision: **[~] pending final independent review and CI for bb60053**.

Deferred to T202: animation clips, tracks, keyframes and playback.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile/fuzz matrix, differential oracle and broader limits.
Deferred to T300/T700: CPM projection, output and broad filesystem/locale matrix.
