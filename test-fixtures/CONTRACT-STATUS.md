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
| GeckoLib animation oracle for A–D | BLOCKED | `scripts/fixture_oracle_report.py` validates the pinned checkout and records input hashes. The S004 runner cannot consume these geometry/animation files without introducing the prohibited production parser; no parse result is claimed. |
| Converted output comparison | NOT APPLICABLE | No production parser, sampler, projection, or writer exists in Phase 1. |

Consequently, a passing fixture audit means that the authorial inputs and
schema-bound mappings satisfy the checks listed above; it does not certify
semantic compilation or GeckoLib runtime acceptance.
