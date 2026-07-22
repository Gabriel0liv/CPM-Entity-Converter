# GeckoLib input boundary (NON_PRODUCTION evidence)

This document records only behavior observed in the GeckoLib 4.4.9 oracle
spike. It is not a production parser contract. The oracle checkout is pinned
to commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5` and the MVP target remains
GeckoLib 4.4.9 on Minecraft 1.20.1/Forge.

## Supported evidence

The S004 fixtures exercised Bedrock-style animation JSON through GeckoLib's
`BakedAnimationsAdapter`, `KeyFramesAdapter`, `Animation`, `Keyframe`,
`KeyframeStack`, `EasingType`, and `MolangParser`. The oracle distinguishes
parser structures from sampled values. The following facts were observed:

- Animation length is expressed in seconds and is converted to ticks; `1.0`
  became `20.0` ticks. With no explicit length, the last keyframe timestamp was
  used when keyframes existed. With neither length nor keyframes, the observed
  sentinel was `Double.MAX_VALUE`.
- Boolean `true` and the string `loop` resolve to `LOOP`; boolean `false`,
  `play_once`, unknown strings, and absent loop values resolve to
  `PLAY_ONCE`. `hold_on_last_frame` resolves to GeckoLib's hold enum. Terminal
  controller behavior was not inferred from the enum alone.
- Keyframe objects use `pre` when present and fall back to `post` when `pre` is
  absent. When both differ, `post` is discarded by the 4.4.9 adapter. The
  converter must report `ANIM_PRE_POST_COLLAPSED_449` with both source values.
- Channel-level `lerp_mode: catmullrom` was ignored by the adapter. A per-
  keyframe `easing: catmullrom` form was recognized and produced the observed
  non-linear samples. Unknown easing fell back to linear in the tested input.
- Linear, step, sine, bounce, back, elastic, and catmullrom keyframe easings
  were parsed/evaluated by the oracle. Back/elastic arguments `[1.2, 0.35]`
  survived parsing. Easing samples are not evidence of a complete controller
  timeline.
- Rotation channels remained scalar Euler values. The adapter converted X and
  Y degrees to negated radians and preserved Z sign. Values such as `190°`,
  `720°`, and the endpoints `350°` → `10°` remained scalar values; no
  quaternion shortest-path operation was observed.
- Position and scale channels were parsed independently. A scale value of zero
  was produced by GeckoLib; whether CPM can represent it is a later projection
  concern.
- Molang numeric constants and the tested constant expression parsed as
  constants. A runtime-dependent variable expression parsed as a non-constant
  value requiring runtime context. Offline conversion therefore accepts only
  constant expressions; dynamic expressions produce
  `ANIM_DYNAMIC_MOLANG_UNSUPPORTED` unless an explicit runtime context is
  provided.
- JSON object insertion order was preserved for the tested keyframe adapter.
  Duplicate JSON properties were resolved by Gson before the adapter and the
  last value survived. Missing channel stacks remained empty.

## Geometry and model input

The S004 oracle is an animation-semantics spike, not a complete geometry
parser. Geometry JSON, cubes, pivots, rotations, box UV, per-face UV, and
entity-specific bone conventions therefore remain input requirements to be
validated by T200's future adapter. No geometry behavior is claimed here as
accepted solely from S004.

## Events and playback limits

Sound/particle timeline structures were parsed by `KeyFramesAdapter` where
covered by the fixtures. A complete mapping of events to CPM is not yet
implemented. Controller terminal observations for `PLAYBACK-001`,
`PLAYBACK-004`, and `PLAYBACK-005` remain `BLOCKED`: the real controller was
instantiated, but observing `D-epsilon`, `D`, `D+epsilon`, and `2D` requires a
complete `CoreGeoModel` integration. No editor or visual result is claimed.

## Explicitly unsupported or deferred

The following are outside the current evidence boundary:

- GeckoLib versions other than 4.4.9;
- production parsing of `.geo.json` or `.animation.json`;
- dynamic Molang evaluation without a supplied runtime context;
- definitive CPM sampling, playback lifecycle, seam handling, root projection,
  head/neck retargeting, writer behavior, and CLI behavior;
- automatic acceptance of arbitrary custom easing or custom loop semantics;
- visual validation in the CPM editor.

These limitations relate to T200–T204 and later phases. This document must be
updated when a production adapter supplies new executable evidence.
# Evidência T200

O parser implementado aceita exclusivamente `format_version: "1.12.0"`,
seleciona `description.identifier` por igualdade exata, preserva a ordem de
bones/cubes e registra JSON pointers. Pivôs e rotações de bind são convertidos
para a convenção do IR (`C(x,y,z)=(-x,-y,+z)` e graus `(-rx,-ry,+rz)`) antes da
criação do quaternion ZYX. `inflate` de cube herda o valor do bone quando
ausente; `mirror` do bone não é aplicado ao cube, conforme observado em
`BakedModelFactory.constructCube` no checkout GeckoLib fixado.

UV é apenas encaminhada como boundary bruto para T201. PNG, animações,
keyframes, Molang, sampling e projeção CPM permanecem deferidos.
