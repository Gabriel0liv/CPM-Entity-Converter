# Gate T303

Status: **[x] PASS**

Base: `8053f49a1fa4453dc6b688514f1d8fd58d3fd554`.
Technical integration HEAD: `69966b15c9705b38f69731271141f2cb9679532c`.
CI-validated gate checkout: `7521804755e2e02a16680da851ebf9fbb57a1866`.
Gate closure commit: `183a1db5135fe41de9b248b0b6606b77f94b9882`.
Metadata correction predecessor: `cbaf83b7a35bdd0c4ce105f9bfe56741d6af0643`.
Feature correction: `9daa295` (merged into integration).

| Evidence | Command/test | Cases | Result | Commit/run |
|---|---|---:|---|---|
| Validator tests | `:validator-cpm:test` | 23 | PASS | `9daa295` |
| Mutation matrix | `CpmArtifactMutationMatrixTest` | 28 executable mutators | PASS | `872def8` |
| Fixture A | `CpmFixtureArtifactTest` | writer + validator, SHA `31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97` | PASS | `872def8` |
| Fixture C | `CpmFixtureArtifactTest` | writer + validator, SHA `177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3` | PASS | `872def8` |
| Fixture B smoke | `CpmFixtureSmokeTest` | writer + validator, SHA `4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9` | PASS | `872def8` |
| Fixture D smoke | `CpmFixtureSmokeTest` | writer + validator, SHA `82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695` | PASS | `872def8` |
| S003 | `s003Evidence` + `CpmS003ArtifactTest` | M0–M5 regenerated and validated | PASS | `9daa295` |
| Formatting | `spotlessCheck` | all configured modules | PASS | `69966b1` |
| Full build | `clean check --no-daemon` | all modules | PASS | `69966b1` |
| Windows CI technical closure | `check` | checkout `7521804755e2e02a16680da851ebf9fbb57a1866` | PASS | run `30122133695`, job `89577060327` |
| Windows CI hygiene technical | `check` | checkout `e4f7cbb2e95483e100cd80f229c04f303ac48ada` | PASS | run `30125213819`, job `89587000983` |
| Windows CI hygiene final | `check` | checkout `2c866970d4f9c90ff3ed87f3f01e3f42f0d41d5a` | PASS | run `30125416328`, job `89587651810` |

Canonicality contract observed from the writer: UTF-8 compact JSON with a final
LF, writer field order and normalized numeric lexemes; config before skin and
sorted animation entries; DEFLATED entries at the fixed 1980-01-01 epoch.
Only metadata exposed by `CpmArtifactEntry` is asserted. Non-canonical artifacts
remain successful with `CPM_NON_CANONICAL` warnings.

UV uses the typed project model and validates logical-grid bounds. PNG support
is RGBA8, non-interlaced, zlib scanlines with filters 0–4 and explicit decoded
budgets. Static STORE_REFERENCES is not applicable in the MVP. Visual
validation was not run. T304 was not started.

T300/T301/T302/T303: `[x]`; T304/T400–T403/T600–T601: `[ ]`.

Commits documentais posteriores podem corrigir metadados sem substituir o
SHA técnico ou o checkout validado pelo gate.
