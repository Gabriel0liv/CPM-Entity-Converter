# Phase 3 T300 Gate

Commit base: `3edfb6d9b4f894acea5708249466557984bddcd5`
Integration branch: `integration/phase3`
Implementation commit: `89cecbf41db6cea422cba7e44eb3749adb320b0e`
Final evidence commit: `51a314f8e3a5ff61531a9fde1c2ae19c8cdb4176`
Integrated HEAD: `f15f70c76835fe9eeadd964346340d5c0ce90249`

Implemented evidence:

- isolated `projection-cpm` module consuming `ModelIR` and compiled
  `SemanticRigMap`;
- immutable logical project/root/element/cube/texture types and projection
  index without numeric store IDs;
- six canonical vanilla roots and `single_anchor` BODY strategy;
- body, descendant bone and cube projection with source order;
- authored-pivot reconstruction, cube offsets, rotated-cube helpers and INFO
  provenance diagnostics;
- quaternion to Euler ZYX decomposition, mirror/inflate and UV transport;
- skin/default/slim and deferred modelScale/verticalOffset preconditions;
- logical validator and focused root/hierarchy tests;
- local `clean check`, manifest, S004 audit and frozen oracle pass.
- CPM-owned UV types now replace `UvIR` in the logical graph; box UV validates
  integral coordinates and cube sizes, while per-face UV preserves signed
  endpoints and canonical face order with UP/DOWN `ROT_180`.
- Authored pivots are resolved recursively with memoization, independent of
  ModelIR list order. Node origins retain source IDs and `SourceLocation`; final
  projection indexes are rebuilt in ModelIR bone/cube order.
- Focused UV and pivot-resolver tests pass.

Windows workflow: initial run `29988754306`, HEAD `ff44e60fa1e083ab80ad8dde7cdf72ee276f982b`, job `89146467746`, PASS. Final run `29990193595`, HEAD `f15f70c76835fe9eeadd964346340d5c0ce90249`, job `89151011633`, PASS.

T300: **[~] in progress**

The full acceptance gate remains open pending the required fixture A/C logical
snapshots, structured B/D smoke coverage, complete UV projection matrix and
the final documentation-only gate review. T301–T304 and T400–T402 remain
unimplemented.
