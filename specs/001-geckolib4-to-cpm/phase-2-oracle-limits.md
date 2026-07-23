# T204 practical limits and frozen oracle

The fixed GeckoLib pin is `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`.
The frozen S004 artifact contains 37 fixtures and 90/90 assertions. The local
adapter keeps deliberately stricter behavior for dynamic Molang, unknown easing,
invalid durations and duplicate raw JSON properties. Terminal playback lifecycle
remains deferred to T401.

The local parser limits are explicit and injectable: geometry limits cover bytes,
JSON depth, geometry/bone/cube counts, hierarchy depth and relevant string
lengths; animation limits cover bytes/files, nesting, clips, bones per clip,
keyframes per channel and in total, string/Molang expression length and easing
argument count. Limit failures return `INPUT_LIMIT_EXCEEDED` with
`limitName`, `limit`, `observed`, a logical pointer and no partial success.
Geometry and animation readers reject duplicate JSON properties and bound nesting
before semantic parsing.

The adapter parity matrix is frozen at 37 inputs: 28 MATCH, 7 STRICT and 2
DEFERRED. MATCH inputs use the same observable baseline semantics, STRICT inputs
are intentionally rejected or diagnosed more conservatively, and DEFERRED inputs
belong to later lifecycle/projection work. The matrix is asserted by
`GeckoOracleParityTest` and the frozen GeckoLib artifact remains the regression
oracle rather than a production parser dependency.

T204 covers bounded geometry, PNG and animation parsing plus the frozen oracle
verification. A real GeckoLib oracle execution at pin
`25a41d7375bb7eeda37dadc04b1e03fe486b33e5` produced 90/90 assertions (33 PASS,
1 EXPECTED_REJECTION and 3 upstream-blocked fixtures, with zero failed
assertions); the checked-in frozen artifact remains unchanged because the run
only refreshed environment metadata. T204 does not introduce fuzzing,
adversarial threat guarantees, sampling, CPM projection, writer or CLI behavior.
