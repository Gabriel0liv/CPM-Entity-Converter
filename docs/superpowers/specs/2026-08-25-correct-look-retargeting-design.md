# Correct Look Retargeting Design

## Goal

Define "correct conversion" as preserving GeckoLib geometry and authored animation while keeping the converted CPM model kinematically correct under player-driven yaw/pitch and animation state changes. A conversion is not accepted merely because the `.cpmproject` opens.

## Correctness levels

1. **Structural correctness**: hierarchy, pivots, cubes, UVs, texture and bind-pose world transforms match the supported Gecko source within tolerance.
2. **Animation correctness**: for every sampled time `t`, the CPM-authored pose matches the Gecko evaluator pose within the configured positional/rotational/scale tolerances. Sampled animation is bounded-equivalent, not mathematically lossless.
3. **Semantic correctness**: player states and dynamic inputs (walk/run/jump/attack/look) drive the intended converted clips/bones without double transforms, discontinuities or hierarchy distortion.

All three are release criteria for a "correct" conversion.

## Synthetic entity root

Single-anchor conversion SHALL introduce one synthetic, geometry-free `entity_root` under the selected CPM vanilla anchor (BODY is the default general anchor for arbitrary entities unless a later accepted ADR changes it).

```text
CPM BODY
└── entity_root
    └── original Gecko hierarchy
```

`entity_root` owns only global projection concerns such as CPM anchor offset, model scale and configured vertical offset. Gecko-local parent/child relationships remain intact below it unless a specific semantic operation requires a world-space-preserving rebake.

This prevents root-space offsets from being mixed with ordinary Gecko local-delta conversion and minimizes rebaking.

## Look composition contract

Dynamic player look is an additive semantic layer applied after the sampled/authored base pose, but the delta must be expressed in the correct local space of the configured look bones.

For each runtime sample:

1. evaluate/reconstruct the base Gecko pose for the active clip(s);
2. compute base world matrices through the preserved hierarchy;
3. derive the requested player look delta from yaw/pitch;
4. clamp the requested look according to mapping limits unless `allowOverrotation` is explicitly enabled;
5. distribute yaw/pitch across configured `neck` and `head` bones using normalized influences;
6. compose each look delta relative to that bone's animated parent/world orientation rather than overwriting its authored rotation;
7. solve the resulting local matrix with `M_local_new = inverse(M_world_parent) × M_world_target`;
8. decompose only if the result is representable as CPM TRS; shear/non-representable transforms are diagnostics, never silently approximated;
9. choose a continuous ZYX Euler branch using the source Euler hint and previous output so ±180° crossings do not snap and authored winding is not destroyed.

Authored head/neck animation must survive look application. Look is never implemented as an unconditional replacement of the head channel.

## Look mapping

The semantic mapping keeps explicit `head`, optional `neck`, `headInfluence`, `neckInfluence`, `allowOverrotation`, and `limits`. Compiled mapping must preserve `limits`; semantic compilation may not silently discard them.

Influence defaults are configuration policy, not hard-coded anatomy. Humanoids, long-neck creatures and quadrupeds may use different splits.

## Matrix/TRS requirements

Phase 1 math must support:

- ZYX Euler -> quaternion -> matrix;
- affine inverse;
- representable affine matrix -> translation + rotation + signed/nonzero scale;
- explicit shear detection;
- matrix/quaternion -> ZYX Euler branch extraction;
- gimbal-lock handling with continuity hint;
- continuity across ±180°;
- preservation of source-authored winding information such as 0° -> 720° (matrix/quaternion alone cannot recover winding, therefore source hints remain part of the IR).

Non-uniform scale combined with reparenting can introduce shear. Such cases are rejected/diagnosed unless a later CPM projection strategy explicitly supports the resulting transform.

## Acceptance matrix for look

Automated and manual acceptance must cover at least:

- idle + yaw/pitch;
- walk + yaw/pitch;
- run + yaw/pitch;
- jump + yaw/pitch;
- attack/custom clip + yaw/pitch;
- animated torso/neck parent + look;
- ±179° crossing without long-path spin;
- configured clamp boundaries;
- neck/head influence split;
- 100 reset/layer cycles without drift;
- a deep hierarchy where head is not a direct child of BODY.

The result fails semantic correctness if the head doubles vanilla rotation, loses the authored clip rotation, snaps at branch boundaries, rotates in the wrong parent space, or causes visible neck/head separation.

## Scope boundary

The converter guarantees the supported visual rig and animation semantics available in Gecko/Bedrock assets plus explicit mapping. It does not infer or reproduce arbitrary Java-side procedural renderer logic, entity AI, hitboxes, physics, attacks, sounds, particles, shaders or other gameplay behavior not represented by the supported inputs.
