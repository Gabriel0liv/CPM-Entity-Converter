# Gate T303

Status: **[~] evidence correction in progress**

Base: `45ec99b75f0ff29e3135ae4dc794576ac7eaa627`.
Integration HEAD validated by CI: `fe827dfde241f01da0f096756820b088c1c2abdd`.
Feature correction: `c856105` (merged into integration).

| Evidence | Command/test | Cases | Result | Commit/run |
|---|---|---:|---|---|
| Validator tests | `:validator-cpm:test` | 23 | PASS | `c856105` |
| Mutation matrix | `CpmArtifactMutationMatrixTest` | 28 executable mutators | PASS | `c5aec17` |
| Fixture A/C | `CpmFixtureArtifactTest` | existing ZIP assembly, not writer pipeline | INCOMPLETE | `c856105` |
| Fixture B/D smoke | `CpmFixtureSmokeTest` | source corpus only; validator not called | INCOMPLETE | `c5aec17` |
| S003 | `s003Evidence` + `CpmS003ArtifactTest` | M0–M5 regenerated and validated | PASS | `c5aec17` |
| Formatting | `spotlessCheck` | all configured modules | PASS | `fe827df` |
| Full build | `clean check --no-daemon` | all modules | PASS | `fe827df` |
| Windows CI | `check` | `e497a6b8b1ecad281b414e65b4a8f3f6fe8ab17d` | PASS | run `30113686363`, job `89549066450` |

Canonicality contract observed from the writer: UTF-8 compact JSON with a final
LF, writer field order and normalized numeric lexemes; config before skin and
sorted animation entries; DEFLATED entries at the fixed 1980-01-01 epoch.
Only metadata exposed by `CpmArtifactEntry` is asserted. Non-canonical artifacts
remain successful with `CPM_NON_CANONICAL` warnings.

UV uses the typed project model and validates logical-grid bounds. PNG support
is RGBA8, non-interlaced, zlib scanlines with filters 0–4 and explicit decoded
budgets. Static STORE_REFERENCES is not applicable in the MVP. Visual
validation was not run. T304 was not started.

T300/T301/T302: `[x]`; T303: `[~]`; T304/T400–T403/T600–T601: `[ ]`.

The technical integration HEAD is `1449c0c30f7e8b4616aad99e8d8374cbe4863f09`;
the final documentation commit is appended after the technical CI. The
external CI run for the technical HEAD is `30116215876`, job `89557414982`,
checkout SHA `1449c0c30f7e8b4616aad99e8d8374cbe4863f09`.
