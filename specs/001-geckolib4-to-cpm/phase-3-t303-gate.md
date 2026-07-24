# Gate T303

Status: **[~] correction in progress**

Base documental: `3e1c3cd3bc2fd493e1c8e73bf1084ad50fa6fa38`.
Feature commits: `1b1b201`, `4b27b2c`.
Integration HEAD: `45d1c35a9eef1e573f0f2ce8d30416941cf5315c`.

| Evidence | Command/test | Cases | Result | Commit/run |
|---|---|---:|---|---|
| Validator suite | `:validator-cpm:test` | 21 | PASS | `4b27b2c` |
| Mutation matrix | `CpmArtifactMutationMatrixTest` | 4 focused representatives | PASS | `4b27b2c` |
| Fixture A/C | `CpmFixtureArtifactTest` | 2 artifacts, deterministic summaries | PASS | `4b27b2c` |
| Summary | fixture artifact validation | roots/elements/targets/texture | PASS | `2ecfccb` |
| PNG | `CpmPngValidatorTest` | RGBA8, filters, profile, trailing bytes | PASS | `f387732` |
| Full build | `clean check --no-daemon` | all modules | PASS | `45d1c35` |
| Windows CI | workflow `check` | Windows | PASS | run `30106891728`, job `89526388035` |

Canonical writer contract: `config.json` precedes `skin.png`, animation
entries follow deterministic name order, entries use DEFLATED, fixed epoch
`1980-01-01T00:00:00`, no comments, and JSON uses UTF-8, LF and a final
newline. Non-canonical artifacts remain successful with `CPM_NON_CANONICAL`.

PNG profile: RGBA8, non-interlaced, zlib scanlines with filter bytes 0–4;
interlaced and other color profiles are rejected. Static STORE_REFERENCES is
not applicable in the MVP. Visual validation was not run. T304 was not started.

Reopened because the previous matrix had only four cases, B/D did not have
dedicated smoke tests, S003 was not re-executed in the gate, JSON canonicality
was incomplete, textureSize/customGrid were not fully represented in UV, and
the gate contained stale CI IDs.

T300/T301/T302: `[x]`; T303: `[~]`; T304/T400–T403/T600–T601: `[ ]`.
