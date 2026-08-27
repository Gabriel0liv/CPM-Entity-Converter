# T205 — GeckoLib 4.4.9 source-feature hardening

Status: **reviewed against pinned default GeckoLib 4.4.9 bake/render path; implementation pending TDD**.

Date: 2026-08-26.
Pinned GeckoLib commit: `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.

## Why T205 exists

The T300 topology review showed that the single-anchor CPM projection is compatible with the actual CPM renderer, but also exposed an older input-boundary problem: the pinned Gecko raw geometry parser recognizes fields that `GeckoGeometryParser` currently neither represents nor rejects. That violates NFR-007 even if those fields are uncommon.

T205 closes that gap before production CPM projection starts. Its job is not to support arbitrary mod Java/render logic. Its job is to make the supported default GeckoLib 4.4.9 JSON semantics explicit and loss-aware.

## Pinned runtime path reviewed

The classification below is based on the default GeckoLib path:

```text
raw geometry JSON
  -> GeometryTree / ModelProperties / Bone / Cube
  -> BakedModelFactory.Builtin
  -> GeoBone / GeoCube
  -> AnimationProcessor
  -> GeoRenderer
```

A mod may register a custom `BakedModelFactory`, custom `GeoModel#setCustomAnimations`, render layers, or mutate bones from Java. Those procedural/custom-code semantics are outside the file-only MVP and remain covered by CON-002. The converter must not claim to reproduce them from `.geo.json`/`.animation.json` alone.

## Bone fields

| Raw field | Pinned default behavior | MVP policy |
|---|---|---|
| `name` | identity / animation lookup | already supported |
| `parent` | hierarchy | already supported |
| `pivot` | baked bone pivot | already supported |
| `rotation` | baked bind rotation | already supported |
| `cubes` | geometry ownership | already supported |
| `inflate` | inherited fallback when a cube omits its own inflate | already supported; regression test required |
| `mirror` | stored on `GeoBone`, but `BakedModelFactory.Builtin.constructCube` uses `cube.mirror()` directly and does not inherit bone mirror into cube bake | do not invent cube inheritance; record explicit occurrence if present until a file-level visual effect is demonstrated |
| `neverRender` | passed to `GeoBone`; constructor initializes `hidden=true`; default `GeoRenderer` skips that bone's cubes but still renders child bones because `childrenHidden` remains false | **support** as bone-level `renderOwnCubes=false`; do not map the structural CPM bone itself to `hidden` |
| `reset` | stored on `GeoBone` and exposed as `getReset()`; no effect is observed in the pinned default `AnimationProcessor` path reviewed | preserve/diagnose as unsupported metadata unless an additional pinned default consumer is demonstrated; never silently drop |
| `bind_pose_rotation` | recognized by raw `Bone`; not consumed by `BakedModelFactory.Builtin.constructBone` | record explicit ignored/default-factory metadata occurrence; no transform effect in the pinned builtin path |
| `debug` | recognized raw metadata; not consumed by builtin bake | record explicit metadata occurrence if present |
| `locators` | recognized raw metadata; not consumed by builtin geometry bake used by the MVP | record as unsupported feature occurrence; custom render/code may consume it, so do not call it converted |
| `render_group_id` | recognized raw metadata; not consumed by builtin bake | record explicit metadata occurrence |
| `texture_meshes` | recognized raw field but not represented by the MVP cube contract | fail with stable unsupported diagnostic when non-empty/non-null unless a later explicit representation is added |
| `poly_mesh` | outside MVP | already rejected explicitly |

### `neverRender` semantic requirement

Pinned 4.4.9 behavior is not equivalent to CPM normal-element hiding:

```text
Gecko neverRender=true
  bone transform still exists
  own cubes are skipped
  child bones still recurse/render
```

The converter therefore needs a representation such as `BoneIR.renderOwnCubes` (exact Java name may vary), default `true`. When raw `neverRender=true`, set it to `false`. T300 then keeps the structural bone node active and marks only visible cube nodes directly owned by that bone as non-rendering. Child structural bones remain unaffected.

This is required for hierarchy correctness.

## Cube fields

Pinned raw `Cube` fields are `inflate`, `mirror`, `origin`, `pivot`, `rotation`, `size`, and `uv`. The current parser already represents these fields.

Important compatibility details:

- cube inflate falls back to bone inflate when cube inflate is absent;
- cube mirror does **not** fall back to bone mirror in `BakedModelFactory.Builtin.constructCube`;
- per-face/box UV orientation remains governed by the cube mirror value and signed UV sizes;
- cube pivot/rotation remains a helper-node concern and must not be flattened.

T205 adds explicit regressions for the first two points so later refactors cannot accidentally implement Bedrock semantics different from the pinned Gecko runtime.

## ModelProperties

Pinned `ModelProperties` recognizes:

- legacy animation flags: `animationArmsDown`, `animationArmsOutFront`, `animationDontShowArmor`, `animationInvertedCrouch`, `animationNoHeadBob`, `animationSingleArmAnimation`, `animationSingleLegAnimation`, `animationStationaryLegs`, `animationStatueOfLibertyArms`, `animationUpsideDown`;
- `preserve_model_pose`;
- `visible_bounds_width`, `visible_bounds_height`, `visible_bounds_offset`;
- `identifier`, `texture_width`, `texture_height`.

`identifier` and texture dimensions are already consumed by the adapter. The reviewed default bake path uses the texture dimensions for UV construction; the other properties are carried in `BakedGeoModel.properties()` rather than changing `BakedModelFactory.Builtin` bone/cube construction.

MVP policy:

- visible-bounds fields are non-mesh metadata for the file-only converter; preserve their occurrence for diagnostics/reporting, not as CPM geometry transforms;
- legacy animation/preserve-pose flags are recorded as unsupported/default-runtime metadata when present. They must not silently affect a claim of semantic equivalence, especially because mod code or renderer/controller behavior may interpret them outside the static bake;
- if future pinned-source evidence demonstrates a default visual/animation effect for one of these flags, it moves from metadata occurrence to represented semantics or conversion error.

## Unknown fields

T205 distinguishes **known Gecko 4.4.9 fields** from arbitrary unknown JSON fields.

- recognized pinned fields follow the explicit policies above;
- an unrecognized field in a bone/cube/description object must not be guessed. The parser should reject it in strict mode or surface a stable unsupported diagnostic according to the existing parser policy;
- raw fields explicitly proved irrelevant to `BakedModelFactory.Builtin` may be retained as `FeatureOccurrence` with provenance instead of failing, provided the report later exposes them.

## IR changes

Minimal preferred IR change:

```text
BoneIR
  ...existing fields...
  renderOwnCubes: boolean = true
```

Keep the field static and semantic. Do not add a generic map of arbitrary Gecko JSON into `BoneIR`.

Metadata/features that are not part of the supported render semantics use existing `ModelIR.unsupportedFeatures : List<FeatureOccurrence>` with deterministic provenance. T403 later reports explicit ignored/unsupported occurrences.

Backward-compatible constructors may default `renderOwnCubes=true` so Phase 1/2 tests remain source-compatible.

## Diagnostics

Add stable adapter diagnostics only where `FeatureOccurrence` alone is not enough:

- unsupported geometry feature that makes faithful output impossible: error;
- recognized metadata/default-bake-inert field: warning or `FeatureOccurrence`, never silent;
- invalid type/value for a recognized field: `GEO_INVALID_VALUE`;
- features dependent on custom Java/runtime code are not approximated.

`texture_meshes` is an error for the MVP when populated. `neverRender` is supported and must not warn merely for being used.

## Required TDD coverage

1. `neverRender=true` produces a bone with `renderOwnCubes=false` while preserving children and cube data;
2. `neverRender=false`/absent defaults to `true`;
3. bone inflate is inherited by cube only when cube inflate is absent;
4. cube inflate overrides bone inflate;
5. bone `mirror=true` does not make an unmirrored cube mirrored under the pinned builtin semantics;
6. cube mirror still works;
7. populated `texture_meshes` fails explicitly;
8. `locators`, `bind_pose_rotation`, `debug`, `render_group_id`, `reset` and bone `mirror` do not disappear silently: they are represented as deterministic feature occurrences or a documented diagnostic;
9. ModelProperties fields outside identifier/texture dimensions likewise become explicit occurrences instead of disappearing;
10. deep hierarchy with `neverRender` on an intermediate bone keeps descendants attached/renderable;
11. fixtures A-D remain unchanged where they do not use these fields;
12. S004 oracle and full Ubuntu/Windows repository CI stay green.

## Release condition for T300

T300 becomes unblocked only when:

- the tests above are GREEN;
- `ModelIrValidator` accepts/preserves the new semantic field;
- diagnostics/FeatureOccurrence order is deterministic;
- existing fixture outputs have intentional diffs only;
- final branch HEAD passes Spotless, `clean check`, reproducibility, fixture manifest and S004 oracle in the established CI matrix.

## Architecture conclusion

T205 does **not** change the approved single-anchor T300 topology. It strengthens the input contract so T300 cannot faithfully preserve a hierarchy that was already semantically damaged during parsing.
