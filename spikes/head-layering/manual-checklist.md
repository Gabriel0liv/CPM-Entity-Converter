# CPM editor manual checklist

> **NON_PRODUCTION** — record CPM version, platform and date for every run.

For each project under both artifact directories:

1. Open the project and confirm there is no loader/editor error.
2. Confirm the neutral pivots and the horn position against `measurements.json`.
3. Play standing, walking and 100 walking loops; record drift or a loop seam.
4. Test minimum, neutral and maximum `HEAD_ROTATION_YAW` and `HEAD_ROTATION_PITCH`.
5. Combine walking with yaw, pitch and both; verify the base animation remains visible.
6. Rotate the body while look is active; record whether head/neck inheritance matches the strategy.
7. Confirm the horn follows the head without an independent translation.
8. Switch standing -> walking -> standing and confirm neutral restoration.
9. Compare normal and equal-priority projects; record effective same-priority ordering.
10. Compare absolute-base/additive-look and additive-base/additive-look.

Do not mark a visual result PASS from the generated mathematical expectation alone.

