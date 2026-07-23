# Phase 2 T204 Gate

Commit base: `efa74004b3692b294f92f7b4b719593a467cb8f7`
Implementation commit: `cd8d4d49b8c08069ea0cc1d1fa68765028d68a08`
Integrated HEAD: `e0c9c34973bb19df8e134cb22e520b2bcb097d9a`
Gate record: this commit

Evidence:

- Geometry, PNG and animation parsers use injectable practical limits. The
  animation limits cover bytes/files, nesting, clips, bones per clip,
  keyframes per channel and in total, string/Molang expression length and
  easing-argument count. Limit diagnostics include `limitName`, `limit`,
  `observed`, logical pointers and no partial success.
- `BoundedJsonReader` enforces duplicate-property rejection and nesting bounds
  for both geometry and animation JSON.
- `GeckoOracleParityTest` freezes the 37-input matrix as 28 MATCH, 7 STRICT and
  2 DEFERRED cases.
- The real GeckoLib oracle execution at pin
  `25a41d7375bb7eeda37dadc04b1e03fe486b33e5` completed with 90/90 assertions,
  33 PASS, 1 EXPECTED_REJECTION and 3 upstream-blocked fixtures, with zero
  failed assertions. The checked-in frozen artifact remains pinned and passes
  its verifier.
- Local `clean check`, reproducible build, manifest check, S004 audit and
  frozen-oracle verification pass.

Windows workflow: run `29982601875`, HEAD
`e0c9c34973bb19df8e134cb22e520b2bcb097d9a`, job `check` /
`89127543560`, PASS.

T204: **[x] PASS**
Phase 2 practical-limits/oracle gate: **PASS**
T300 remains deferred and not implemented.

Deferred: fuzzing/adversarial guarantees, sampling, lifecycle, pose mapping,
CPM projection, writer and CLI remain outside T204.
