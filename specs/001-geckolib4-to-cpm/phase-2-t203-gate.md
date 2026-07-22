# Phase 2 T203 Gate

Commit base: 1a9ec079ad2010772e42d3f7a8f9692913e2304c
Implementation branch: feature/t203-easing-molang
Implementation commit: 4a51f52f6157211bbe3efe9c9ff28295adbade68
Implementation HEAD: 75eaef5f3dc858bae549d9aa4558731d5b990118
Windows workflow: 29966535553 — HEAD `75eaef5f3dc858bae549d9aa4558731d5b990118` — job `check` / `89079163086` — PASS
Status: **[x] PASS**

Implemented: EasingKindIR/EasingIR, easingFromPrevious, built-in name
mapping and evaluator, constant offline Molang, dynamic/parse diagnostics,
focused evaluator/parser tests, and updated A/B snapshots.

Deferred to T204: hostile/fuzz inputs and differential oracle.
Deferred to T300/T400/T401/T402: projection, sampling, lifecycle and pose mapping.
