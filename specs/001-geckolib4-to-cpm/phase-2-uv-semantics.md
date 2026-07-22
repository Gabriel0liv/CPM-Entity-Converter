# T201 UV semantics

T201 preserves box and per-face UV values as finite doubles. Negative and
fractional coordinates and signed `uv_size` values are retained; bounds are
diagnostics and never clamped. Per-face maps use the canonical order
`NORTH, SOUTH, EAST, WEST, UP, DOWN`, while omitted faces remain omitted.

`RawUvBoundary` remains adapter-only. `BoxUvIR` and `PerFaceUvIR` are the core
boundary consumed by the static assembler. Animation, easing, Molang, sampling,
CPM projection and output packaging remain deferred.
