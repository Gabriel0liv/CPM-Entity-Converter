# T201 UV semantics

Evidence was read from the pinned GeckoLibOracle checkout (`25a41d7`, tree
`c710982`) and the CPM checkout (`9272f4f`, tree `3c2889a`). The adapter keeps
the Gecko representation separate from CPM projection: `RawUvBoundary` is
adapter-only, while `BoxUvIR` and `PerFaceUvIR` are the static ModelIR boundary.

For a box with base `(u,v)` and floored dimensions `x=floor(size.x)`,
`y=floor(size.y)`, `z=floor(size.z)`, the tested Gecko layout is:

| Gecko face | CubeFaceIR | Rectangle `(u,v,width,height)` | Mirror policy | T300 note |
|---|---|---|---|---|
| west | WEST | `(u+z+x, v+z, z, y)` | preserve CubeIR.mirror | CPM projection may rotate/export |
| east | EAST | `(u, v+z, z, y)` | preserve CubeIR.mirror | deferred |
| north | NORTH | `(u+z, v+z, x, y)` | preserve CubeIR.mirror | deferred |
| south | SOUTH | `(u+z+x+z, v+z, x, y)` | preserve CubeIR.mirror | deferred |
| up | UP | `(u+z, v, x, z)` | preserve CubeIR.mirror | deferred |
| down | DOWN | `(u+z+x, v+z, x, -z)` | preserve CubeIR.mirror | deferred |

These formulas correspond to the observed GeckoLib `UVFaces.fromDirection`,
`BakedModelFactory.buildQuad`, and `GeoQuad` boundary. The CPM exporter’s
`PerFaceUV`/`convertUV` orientation rules are not applied here.

Per-face keys map by the exact lower-case labels `north/south/east/west/up/down`.
`PerFaceUvIR` has defensive storage and deterministic iteration order
`NORTH, SOUTH, EAST, WEST, UP, DOWN`. Faces may be omitted. `u`, `v`, width and
height remain finite doubles, including fractional and negative signed sizes;
mirror is never baked into UV values.

Bounds evaluate every derived rectangle independently using both endpoints;
signed dimensions are not clamped or rounded. Out-of-bounds results are
warnings and retain the original values. Missing, malformed, unknown-face and
unsupported material-instance inputs produce the documented UV diagnostics.

PNG validation and static assembly enforce positive geometry/PNG dimensions and
logical source paths. Animation, easing, Molang, sampling, CPM projection and
output packaging remain deferred to T202, T203 and T300.
