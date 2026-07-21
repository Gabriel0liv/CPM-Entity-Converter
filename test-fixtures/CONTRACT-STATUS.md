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
| `MappingCompiler` against a fixture-backed `ModelIR` | PENDING | This worktree is restricted to `test-fixtures`; no fixture parser or ModelIR construction is implemented here. |
| GeckoLib animation oracle for A–D | PENDING | The GeckoLib oracle is a separate spike and is invoked by its dedicated runner, not by this test-fixtures module. |
| Converted output comparison | NOT APPLICABLE | No production parser, sampler, projection, or writer exists in Phase 1. |

Consequently, a passing fixture audit means that the authorial inputs and
schema-bound mappings satisfy the checks listed above; it does not certify
semantic compilation or GeckoLib runtime acceptance.
