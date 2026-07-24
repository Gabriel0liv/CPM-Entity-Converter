# Gate T304

Status: **[~] AUTOMATED PASS — MANUAL VISUAL PENDING**

Base: `f390a6b8a73cad465dbabc37aae161c4281ceca7`  
CPM: `0.6.27`, commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` (tree `3c2889a8`).

## Automated conformance

| Evidence | Command/test | Result |
|---|---|---|
| Official artifacts | `t304GenerateArtifacts -PcpmReferenceDir=...` | PASS; A/B/C/D generated twice with pinned hashes |
| ProjectIO load | `t304ProjectIoConformance -PcpmReferenceDir=...` | PASS for A/B/C/D and M2–M5; controlled failure for M0/M1 |
| IDs/references | `CpmProjectIoIdentityConformanceTest` | PASS; persisted IDs and M5 references observed in loaded Editor |
| UV/texture | `CpmProjectIoUvConformanceTest` | PASS; loaded texture flags, textureSize and UV origins present |
| Hierarchy/bind | `CpmProjectIoBindTransformTest` | PASS; parent paths and local transforms materialized |
| Round-trip | `CpmProjectIoRoundTripTest` | PASS where ProjectIO save/reopen is available |
| Determinism | `CpmCrossPlatformGoldenTest` | PASS against pinned A/B/C/D SHA-256 values |
| S003 | `s003Evidence` and M0–M5 in report | PASS with M0/M1 expected failures |

Pinned artifact hashes:

- A `31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97`
- B `4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9`
- C `177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3`
- D `82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695`

Outputs are reproducible under `build/t304/`:

- `artifacts/*.cpmproject` and `manifest.json`;
- `projectio/projectio-report.json`;
- `manual-evidence/` bundle.

The verification-only module compiles CPM sources from the external checkout;
no production module depends on CPM. It verifies the exact CPM commit before
compiling. Cross-platform execution is a CI follow-up; the local Windows run
is recorded here without claiming Linux execution.

Automated round-trip uses `ProjectIO.saveProject`/`ProjectFile.save` into a
temporary file and reopens it. No original artifact is modified.

## Manual visual conformance

| Fixture | Open | Save/reopen | Texture/UV | Hierarchy | Evidence |
|---|---|---|---|---|---|
| A | NOT RUN | NOT RUN | NOT RUN | NOT RUN | pending human session |
| B | NOT RUN | NOT RUN | NOT RUN | NOT RUN | pending human session |
| C | NOT RUN | NOT RUN | NOT RUN | NOT RUN | pending human session |
| D | NOT RUN | NOT RUN | NOT RUN | NOT RUN | pending human session |

Tester, date, operating system, screenshots, and observed results remain
blank until a human opens the artifacts in CPM Editor 0.6.27. This gate does
not mark AC-021/023–027, head/neck retargeting, animation projection, T400+,
or visual validation as complete.
