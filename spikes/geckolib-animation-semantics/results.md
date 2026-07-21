# S004 results (NON_PRODUCTION)

The generator creates 37 original fixtures. The real GeckoLib 4.4.9 core jar
and Forge JSON adapters were executed at commit
`25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.

## Oracle execution

`artifacts/results.json` contains 37 parser results with structured assertions.
The run used GeckoLib's actual
`BakedAnimationsAdapter`, `KeyFramesAdapter`, `MolangParser`, `Animation`,
`Keyframe`, `KeyframeStack`, `EasingType` and `Animation.LoopType` classes.
Only `GsonHelper`, logger and `JsonUtil` helper shims were supplied for APIs
normally provided by Minecraft/Forge; no GeckoLib semantics were reimplemented.

## Confirmed observations

- `animation_length` is seconds converted to ticks (`1.0 → 20.0`). Missing
  length with keyframes uses the last keyframe; explicit `0.5` remains `10.0`.
- `pre` wins over `post`; `post` is used only when `pre` is absent. Equal values
  are identical. Differing `pre`/`post` discards `post`; the future converter
  diagnostic is `ANIM_PRE_POST_COLLAPSED_449`. Missing both/vector fails with
  `JsonParseException` (`PREPOST-005`).
- Channel `lerp_mode: catmullrom` is skipped by the adapter (`LERP-001`). The
  per-keyframe `easing: catmullrom` form is recognized (`LERP-002`) and produces
  the observed non-linear/overshooting samples. Future diagnostic for the
  channel-level form: `ANIM_LERP_MODE_IGNORED_449`.
- Linear samples at 0/25/50/75/100% are linear. `step` holds the previous value
  through the midpoint and changes on the positive side. `easeinsine` is
  non-linear. Unknown easing falls back to linear.
- Boolean `true` and string `loop` resolve to `LOOP`; `false`, `play_once`,
  unknown strings and absent values resolve to `PLAY_ONCE`. The string
  `hold_on_last_frame` resolves to the real `HOLD_ON_LAST_FRAME` enum. Controller
  pause/terminal behavior is not claimed from enum parsing alone.
- Missing duration with keyframes uses the last keyframe (`LENGTH-002`); missing
  duration with no keyframes produces the observed `Double.MAX_VALUE` sentinel
  (`LENGTH-003`). A shorter explicit duration is retained (`LENGTH-004`).
- Rotation constants are converted by the real adapter to radians with X/Y
  negated and Z preserved. `190°`, `720°`, and `350°→10°` remain scalar endpoint
  values; no quaternion shortest-path normalization is introduced.
- `KEYFRAME-001` preserves textual insertion order in the adapter (no sort was
  observed); `KEYFRAME-002` demonstrates Gson's last-value-wins behavior for the
  duplicate `0.5` property, so the adapter receives only one entry. `KEYFRAME-003`
  produces independent 3/2/2-keyframe stacks; `KEYFRAME-004` leaves absent
  stacks empty.
- Easing names and arguments are recorded from the real `Keyframe`: linear,
  step, sine, catmullrom, bounce, back and elastic variants; back/elastic
  argument vectors `[1.2, 0.35]` survived parsing and are included in the JSON.
- Position and scale channels parse independently. Scale zero is produced by
  GeckoLib; CPM representability remains a later projection concern.
- Molang constants and constant expressions parse through the real parser.
  Dynamic variables parse into values requiring runtime context; future policy is
  `ANIM_DYNAMIC_MOLANG_UNSUPPORTED` unless context support is explicit.

## Assertions e estados

`assertionsTotal=90`, `assertionsPassed=90`, `assertionsFailed=0`,
`fixturesPassed=33`, `fixturesExpectedRejection=1`, `fixturesFailed=0` e
`fixturesBlocked=3`. `PREPOST-005` é a rejeição esperada. Os três BLOCKED são
exclusivamente a etapa terminal de controller para `PLAYBACK-001`,
`PLAYBACK-004` e `PLAYBACK-005`: o `AnimationController` real foi instanciado e
`shouldPlayAgain` executado, mas observar `D-ε`, `D`, `D+ε` e `2D` requer um
`CoreGeoModel` completo. Ausência de exceção não é usada como PASS.

The report preserves source JSON, input hashes, parser structures, per-axis
keyframe lengths/values, easing names and observed samples. It separates parser
output from the expected policy and does not claim editor/visual behavior or a
full controller tick simulation.

## Source references

- `Forge/.../BakedAnimationsAdapter.java` — animation/keyframe parser.
- `Forge/.../KeyFramesAdapter.java` — sound/particle/timeline keyframes.
- `core/.../animation/Animation.java` — loop type resolution.
- `core/.../animation/EasingType.java` — easing evaluation.
- `core/.../keyframe/Keyframe.java` and `KeyframeStack.java` — runtime structures.
