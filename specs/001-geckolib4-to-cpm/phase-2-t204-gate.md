# Phase 2 T204 Gate

Commit base: 5a188e79c88f96abb435d47c1f17152ad9e5b939
Implementation commit: acc540b4b6dbbf6649e6289cd6c0ad025758d661
Integrated HEAD: a49810c63f69d3540c7fba44354fc870baec6c4d

Evidence:

- `AnimationParserLimits` and bounded file/count checks are active;
- frozen oracle verifier passes 37 fixtures and 90/90 assertions at GeckoLib
  pin `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`;
- geometry and PNG limits remain covered by the existing suites;
- strict duplicate policy and practical local limits are documented;
- clean check, manifest and S004 audit pass.

Windows workflow: run `29968398114`, HEAD `a49810c63f69d3540c7fba44354fc870baec6c4d`, job `check` / `89084781404`, PASS.

T204: **[~] incomplete**
Phase 2: **not closed**
Remaining concrete gaps: maxNestingDepth/maxKeyframes/maxTotalKeyframes/string/Molang/easing-args enforcement, preventive geometry limits, adapter parity matrix and one real GeckoLib oracle execution.
T300 remains deferred until T204 is complete.

Deferred: fuzzing/adversarial guarantees, sampling, lifecycle, pose mapping,
CPM projection, writer and CLI remain outside T204.
