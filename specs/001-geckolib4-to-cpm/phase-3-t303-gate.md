# Gate T303

Status: **[~] final evidence in progress**

Feature commits: `2ecfccb`, `f387732`, `17dcc9d`.
Integration HEAD: `7c4b81ef505fd3aa81a23fdb4cf3c3caec7ab765`.

Evidence includes typed project parsing, root identity and target registry,
independent animation reference validation, typed UV bounds, texture/PNG
relation, RGBA8 PNG stream validation with pixel/decoded budgets, summary
counts, canonicality diagnostics, and full `clean check`.

Windows workflow: `30097092903`.
Windows job: `89493712903` (`check`, PASS).

T300/T301/T302: `[x]`; T303: `[~]`; T304: `[ ]`.

| Evidence | Command/test | Result | Commit/run |
|---|---|---|---|
| Validator tests | `:validator-cpm:test` | PASS | `83fe5cd` |
| Full build | `clean check --no-daemon` | PASS | `83fe5cd` |
| A/C fixture harness | `CpmFixtureArtifactTest` | PASS | `83fe5cd` |
| Canonicality mutation | `CpmArtifactValidatorTest` | PASS | `83fe5cd` |
| Windows CI | `check` | PASS | run `30100256520`, job `89504216645` |

Remaining evidence before T303 can be marked `[x]`: dedicated B/D smoke
artifacts, complete S003 execution report, focused mutation matrix, and
normative writer-derived ZIP/JSON canonicality checks. Visual validation was
not run. Static STORE_REFERENCES remains not applicable in the MVP. PNG
support is limited to RGBA8 non-interlaced streams.
Sampling, retargeting, ProjectIO T304, CLI and T400–T403/T600–T601 were not implemented.
