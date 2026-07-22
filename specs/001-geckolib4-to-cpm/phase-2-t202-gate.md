# Phase 2 T202 Gate

Date: 2026-07-22
Commit base: 93a0ae16c7eda6da4442307f71e616d3a7bc0135
Initial implementation: 080a6e0
Initial integration: fd8e96d
P2.12 corrections: 604e1dc
P2.13 evidence: 3c431c8, 4e010bf
Implementation HEAD: 4e010bffabdd7748492c3adb83314d2325fbaaf4
Windows workflow: pending for final integrated HEAD

Evidence:

- playback is covered parametrically for the supported GeckoLib modes and
  rejected custom/invalid values;
- explicit and inferred durations, duration errors, channel scalar/vector/map,
  ordered timestamps, duplicate timestamps, and absent channels are covered;
- position uses the coordinate change, rotation preserves authored Euler values,
  and scale preserves authored values and components;
- pre/post array and object/vector forms are normalized for comparison; Molang
  components are classified as deferred and structural errors remain distinct;
- `lerp_mode` emits the 4.4.9 warning, easing/easingArgs remain deferred to T203,
  and scoped sound/particle/timeline events are preserved as warnings;
- A and B have integral test-only animation snapshots; C and D have structured
  assertions for clip IDs, playback, duration, track order, BoneId resolution,
  and keyframes;
- manifest check, S004 audit and local clean check pass.

T202: **[~] technical evidence PASS; final Windows workflow pending**

Deferred to T203: easing evaluation and Molang semantics.
Deferred to T204: hostile/fuzz inputs and differential oracle.
Deferred to T300/T400/T401/T402: CPM projection, sampling, lifecycle and pose mapping.
