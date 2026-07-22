# Fixture contract status (NON_PRODUCTION)

This directory contains authorial GeckoLib/Bedrock-shaped inputs and executable
structural contracts. The checks in `scripts/manifest.py` validate JSON shape,
PNG/IHDR metadata, UV bounds, hierarchy, animation channel references,
provenance, and the expected contract files. `FixtureManifestTest` additionally
loads each YAML mapping through the production `MappingLoader`, so the versioned
JSON Schema 2020-12 is executed before binding.

Phase 1 owns authoring, provenance, schema/mapping boundaries and the pinned
animation oracle.  Production geometry/animation parsing and projection are
explicitly deferred to later tasks; this package must not emulate them.

| Gate | Status | Reason |
| --- | --- | --- |
| Authorial fixtures and provenance | PASS | `manifest.py` and `fixture_semantic_contracts.py` verify required files, hashes, dimensions, provenance markers and the authorial inventory. |
| MappingLoader/schema | PASS | `FixtureManifestTest` loads every YAML mapping through the production schema-backed loader. |
| `MappingCompiler` fixture contract | PASS | `FixtureSemanticContractTest` builds a minimal FIXTURE_ONLY IR only for mapping name resolution and compares the canonical compiled snapshot. It is not a geometry or animation parser. |
| GeckoLib animation oracle for A–D | PASS | `scripts/fixture_oracle_report.py --write/--check` executes the pinned GeckoLib Gradle oracle against all four animation files, iterates every clip, compares clip names, bones, channels, loop and duration, and records 41 real assertions plus commit/tree/input hashes. |
| Production geometry parsing | DEFERRED_TO_T200 | No production parser exists in Phase 1. |
| Production texture/UV parsing | DEFERRED_TO_T201 | PNG/UV conversion is not a Phase 1 implementation. |
| Production animation parsing | DEFERRED_TO_T202 | No production animation parser exists in Phase 1. |
| Converted output comparison | NOT_APPLICABLE | Writer/projection work is later-phase scope. |

`scripts/fixture_semantic_contracts.py` is a NON_PRODUCTION/FIXTURE_ONLY
authorial inventory and provenance audit. It intentionally does not claim to
derive a production ModelIR, parser invariants or mutation coverage. Those
contracts are assigned to T200 (geometry), T201 (texture/UV), T202
(animation) and T204 (oracle/parity/hostile-input testing).
