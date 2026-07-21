# S004 experiment plan (NON_PRODUCTION)

Each fixture is fed to the real `BakedAnimationsAdapter` and, where a runtime
driver is available, sampled through GeckoLib keyframe evaluation. The report
must retain source JSON, parser structures, samples, source method, and an
explicit pass/fail/blocked status. Static source inspection is recorded only as
provenance, never as an observed runtime result.

Required groups: PREPOST, LERP, EASE, MOLANG, PLAYBACK, LENGTH, KEYFRAME,
ROTATION, POSITION, and SCALE (see `fixtures/`).
