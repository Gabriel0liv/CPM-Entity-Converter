# Phase 2 T202 Gate

Date: 2026-07-22
Commit base: 7706a58e857f2a3b4e4d5b8cdd10e4c705b88910
Implementation branch: feature/t202-final
Integrated HEAD: 2147b8478c645fe6d8552b1b4de627e78102ae93
Intermediate workflow: 29955716041 (Windows PASS on 7686d48)
Current workflow: pending for integrated HEAD

## Implemented corrections

- tracks are ordered by the source order of `ModelIR.bones()`, not a hash map;
- position and scale channels carry `position`/`scale` components and their
  required modes;
- `lerp_mode` emits the GeckoLib 4.4.9 warning without becoming a timestamp;
- easing and easingArgs are rejected and deferred to T203;
- textual values are classified as deferred Molang while structural values use
  channel diagnostics;
- pre/post vector forms are supported with GeckoLib precedence;
- invalid bones/durations and empty tracks are rejected;
- ModelIrValidator checks duplicate tracks, empty tracks, channel components and
  ZYX rotation order;
- focused A–D integration and semantic parser tests pass locally.

## Gate status

T202: **[~] partial**

The parser corrections and local `clean check` pass, but the frozen checklist
still lacks dedicated parametrized playback/duration/channel coverage, complete
animation snapshots A/B and structured C/D assertions. T202 must remain partial
until those evidence items and the Windows workflow for the integrated commit
are complete.

Deferred to T203: easing evaluation and Molang semantics.
Deferred to T204: hostile/fuzz matrix, integrated limits and differential oracle.
Deferred to T300/T400/T401/T402: CPM projection, sampling, lifecycle and pose mapping.
