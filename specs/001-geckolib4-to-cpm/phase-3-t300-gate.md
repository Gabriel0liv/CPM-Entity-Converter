# Phase 3 T300 Gate

Commit base: `3edfb6d9b4f894acea5708249466557984bddcd5`
Integration branch: `integration/phase3`
Implementation commit: `89cecbf41db6cea422cba7e44eb3749adb320b0e`
Integrated HEAD: `ff44e60fa1e083ab80ad8dde7cdf72ee276f982b`

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

Windows workflow: run `29988754306`, HEAD
`ff44e60fa1e083ab80ad8dde7cdf72ee276f982b`, job `89146467746`, PASS.

T300: **[~] in progress**

The full acceptance gate remains open pending the required fixture A/C logical
snapshots, structured B/D smoke coverage, complete UV projection matrix and
the final documentation-only gate review. T301–T304 and T400–T402 remain
unimplemented.
