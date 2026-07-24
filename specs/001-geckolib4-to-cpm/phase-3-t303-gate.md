# Gate T303

Status: **[~] evidence correction in progress**

Base: `8053f49a1fa4453dc6b688514f1d8fd58d3fd554`.
Technical integration HEAD: `69966b15c9705b38f69731271141f2cb9679532c`.
Feature correction: `9daa295` (merged into integration).

| Evidence | Command/test | Cases | Result | Commit/run |
|---|---|---:|---|---|
| Validator tests | `:validator-cpm:test` | 23 | PASS | `9daa295` |
| Mutation matrix | `CpmArtifactMutationMatrixTest` | 28 executable mutators | PASS | `9daa295` |
| Fixture A | `CpmFixtureArtifactTest` | writer + validator, SHA `31fa2370...` | PASS | `9daa295` |
| Fixture C | `CpmFixtureArtifactTest` | writer + validator, SHA `177d2f33...` | PASS | `9daa295` |
| Fixture B smoke | `CpmFixtureSmokeTest` | writer + validator, SHA `4390f540...` | PASS | `9daa295` |
| Fixture D smoke | `CpmFixtureSmokeTest` | writer + validator, SHA `82384684...` | PASS | `9daa295` |
| S003 | `s003Evidence` + `CpmS003ArtifactTest` | M0–M5 regenerated and validated | PASS | `9daa295` |
| Formatting | `spotlessCheck` | all configured modules | PASS | `69966b1` |
| Full build | `clean check --no-daemon` | all modules | PASS | `69966b1` |
| Windows CI checkpoint | `check` | prior technical HEAD only | PASS | run `30116627187`, job `89558779672` |

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

The previous CI checkpoint `30116627187` validated `8053f49`; it is not the
final gate for this technical integration. T303 remains `[~]` until a new CI
run validates `69966b15c9705b38f69731271141f2cb9679532c`.
