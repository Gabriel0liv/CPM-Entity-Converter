# Gate T303

Status: **[~] correction in progress**

Base: `8030b42e9250975214cc5a0a8a73819d13d7eb5a`.
Integration HEAD: `1e74a9ca307b1af39f45447ecea30e00b5225f0c`.
Feature correction: `a0bd6c3`.

| Evidence | Command/test | Cases | Result | Commit/run |
|---|---|---:|---|---|
| Validator tests | `:validator-cpm:test` | 21 | PASS | `4b27b2c` |
| Mutation matrix | `CpmArtifactMutationMatrixTest` | 4 focused cases | PASS | `4b27b2c` |
| Fixture A/C | `CpmFixtureArtifactTest` | 2 deterministic artifacts | PASS | `4b27b2c` |
| S003 | `CpmS003ArtifactTest` | M0–M5 harness coverage | PASS | `45d1c35` |
| Full build | `clean check --no-daemon` | all modules | PASS | `1e74a9c` |
| Windows CI | `check` | integrated HEAD | PASS | run `30111152627`, job `89540712618` |

Canonicality contract: UTF-8 JSON, LF and final newline; config before skin;
animation entries deterministic; DEFLATED entries; fixed writer epoch; no
comments or unexpected entries. Non-canonical artifacts remain successful with
`CPM_NON_CANONICAL` warnings.

UV uses the typed project model and validates logical-grid bounds. PNG support
is RGBA8, non-interlaced, zlib scanlines with filters 0–4 and explicit decoded
budgets. Static STORE_REFERENCES is not applicable in the MVP. Visual
validation was not run. T304 was not started.

T300/T301/T302/T303: `[x]`; T304/T400–T403/T600–T601: `[ ]`.
