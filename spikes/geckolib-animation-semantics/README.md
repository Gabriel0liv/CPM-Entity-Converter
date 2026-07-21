# S004 — GeckoLib 4.4.9 animation semantics (NON_PRODUCTION)

This spike is an executable-oracle harness. Fixtures are original, tiny JSON
inputs; no GeckoLib source is copied into this repository. The oracle checkout
is pinned to `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.

The oracle uses the real GeckoLib core jar built from the pinned checkout and
the real Forge `BakedAnimationsAdapter`/`KeyFramesAdapter` sources. Minecraft
API calls needed only for JSON helpers are supplied by tiny NON_PRODUCTION shims;
GeckoLib animation, keyframe, easing and loop classes are not reimplemented.

Run from this directory (Gradle 8.8 shown here):

```text
python scripts/generate_fixtures.py
python scripts/audit_fixtures.py
python scripts/run_oracle.py --geckolib-dir <checkout> --gradle <gradle.bat>
```

The runner records parser structures and samples separately from expected
policies. If Java/Gradle is unavailable it emits an explicit `BLOCKED` record;
it never substitutes a Python implementation for GeckoLib semantics.

NON_PRODUCTION: spike code and fixtures only.
