# Phase 2 T202 Gate

Date: 2026-07-22
Commit base: 93a0ae16c7eda6da4442307f71e616d3a7bc0135
Initial implementation: 080a6e0
Initial integration: fd8e96d
P2.12 corrections: 92e4230, 3c71d82, 0d406d2, d8dbd23, 6f2d29a
P2.13 evidence: 3c431c8, 4e010bf
Gate evidence commit: 0853753

Final Windows workflow:

- run 29960011207 — HEAD `0853753826d18f630de5a22fcbbbda2e88f2f4e7` — job
  `check` / `89058520258` — PASS.
- implementation workflow 29959638934 — HEAD
  `4e010bffabdd7748492c3adb83314d2325fbaaf4` — job `check` /
  `89057291915` — PASS.

Evidence accepted:

- playback, explicit/inferred duration, scalar/vector/map channels and ordered
  timestamps;
- pre/post normalization, Molang correctly deferred, easing correctly
  deferred, and `lerp_mode` diagnosed;
- integral snapshots A/B and structured fixture C/D assertions;
- 83 adapter tests, ModelIrValidator, manifest, S004 audit, clean check and
  Windows CI.

T202: **[x] PASS**

Deferred to T203: real easing evaluation and constant/dynamic Molang.
Deferred to T204: hostile/fuzz inputs and differential oracle.
Deferred to T300/T400/T401/T402: CPM projection, sampling, lifecycle and pose mapping.
