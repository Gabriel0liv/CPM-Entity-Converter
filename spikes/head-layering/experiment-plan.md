# S001/S002 experiment plan

> **NON_PRODUCTION**

## Fixed authorial fixture

The fixture has non-zero body/neck/head rotations, distinct neck/head pivots, an offset horn, subtle idle head rotation and walking head bob. The same numeric source is projected into both topologies.

## Controlled variants

Each topology contains deterministic projects for normal priorities (`base=0`, `look=1`), equal priorities, head-only look, `neck/head` splits `0.35/0.65` and `0.5/0.5`, an overrotation split `0.75/0.75`, and additive-base plus additive-look. Absolute-base plus additive-look is the default.

## Cases

The measurement file covers: neutral; idle; walking; neutral/min/max yaw; min/max pitch; walking with yaw, pitch, or both; head-only; the three neck splits; horn inheritance; 100 loops; standing/walking transitions; body rotation with look; equal priority; absolute/additive base; and additive/additive base.

Automated evidence establishes structural references, deterministic files and stateless transform calculations. Visual/editor behavior is not claimed until `manual-checklist.md` is completed.

