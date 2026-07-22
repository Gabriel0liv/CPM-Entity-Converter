# Phase 2 T202 Gate

Date: 2026-07-22
Commit base: 7706a58e857f2a3b4e4d5b8cdd10e4c705b88910
Implementation commit: 080a6e02ff446e5ff7425f8937e05c671d66a4ba
Integration test commit: fd8e96d
Windows profile/CI: implemented in `.github/workflows/ci.yml`
Windows workflow: not yet dispatched for T202

## Implemented boundary

The adapter accepts animation JSON baseline `1.8.0`, resolves exact clip and
bone IDs, supports multiple files, playback modes, explicit/inferred duration,
scalar/vector/timestamp channels, pre/post values, position basis conversion,
source Euler rotation, scale channels, ignored event warnings and deferred
Molang/easing diagnostics. `GeckoAnimatedModelAssembler` replaces only clips
and revalidates ModelIR.

Fixture A–D integration smoke exists in `GeckoAnimationParserTest`; local
`clean check`, core tests and adapter tests pass.

## Gate status

T202: **[~] partial**

The implementation still needs the dedicated parser/playback/channel test
matrix, animation snapshots A/B, structured C/D assertions and a Windows CI run
on the integrated T202 commit before `[x]` is justified.

Deferred to T203: complete easing and Molang semantics.
Deferred to T204: hostile/fuzz matrix and differential oracle.
Deferred to T300/T400/T401/T402: CPM projection, sampling, lifecycle and pose mapping.
