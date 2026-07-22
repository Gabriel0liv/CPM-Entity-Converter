# Fixture contract status (NON_PRODUCTION)

This directory contains authorial GeckoLib/Bedrock-shaped inputs and executable
structural contracts. The checks in `scripts/manifest.py` validate JSON shape,
PNG/IHDR metadata, UV bounds, hierarchy, animation channel references,
provenance, and the expected contract files. `FixtureManifestTest` additionally
loads each YAML mapping through the production `MappingLoader`, so the versioned
JSON Schema 2020-12 is executed before binding.

The following gates are intentionally **pending** and must not be reported as
passed by this fixture package:

| Gate | Status | Reason |
| --- | --- | --- |
| `MappingCompiler` against a fixture-backed `ModelIR` | PASS | `FixtureSemanticContractTest` builds a deterministic FIXTURE_ONLY ModelIR, loads each mapping through the schema-backed MappingLoader, compiles it and compares the complete canonical snapshot. |
| GeckoLib animation oracle for A–D | PASS | `scripts/fixture_oracle_report.py --write/--check` executes the pinned GeckoLib Gradle oracle against all four animation files, iterates every clip, compares clip names, bones, channels, loop and duration, and records 41 real assertions plus commit/tree/input hashes. |
| Converted output comparison | NOT APPLICABLE | No production parser, sampler, projection, or writer exists in Phase 1. |

`scripts/fixture_semantic_contracts.py` is a NON_PRODUCTION/FIXTURE_ONLY
evaluator that derives geometry, animation tracks, source order, UV and
hierarchy invariants from the input files and runs mutation checks. Converted
output remains outside the Phase 1 scope.
