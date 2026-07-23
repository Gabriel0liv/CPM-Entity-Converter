# Fase 3 — Gate T302

Status: **[x] PASS**

Base: `f9e7330a03612cd4cc502f9c05ea8639dfcfdf02`
Implementation: `022746e` (integrated in `075599f`)
P3.06 evidence: `ace201f` (integrated in `f9e7330`)
P3.07 acceptance evidence: `a318cb9`
Integration HEAD reviewed: `a318cb936dfc0ae56f71a100573e0295d050624f`

The writer emits canonical CPM V1 JSON and a two-entry deterministic ZIP
(`config.json`, `skin.png`) with DEFLATED entries, fixed 1980-01-01 metadata,
Locale.ROOT face labels, and byte-preserved texture content. The timezone test
compares complete ZIP bytes under UTC, Europe/Lisbon and
America/Los_Angeles. Any extended timestamp normalization is structural and
limited to local/central ZIP headers; compressed payload bytes are never
scanned. A payload containing the former byte sequence is covered by the test.

Acceptance evidence:

- A/C real pipelines parse geometry, PNG, animations, mapping, projection,
  store-ID assignment and writer output; canonical config and artifact-manifest
  goldens are versioned.
- B/D real pipelines are smoke-tested for successful output, two entries,
  texture byte parity and repeat-run artifact byte equality.
- `CpmArtifactInspector` is test-only, checks entry order, names, method,
  fixed time, uncompressed size, CRC and defensive content copies.
- Graph/JSON element count and registry/store-ID count parity are asserted;
  config contains no absolute paths or runtime-only IDs.
- `clean check`, reproducible build, fixture manifest, S004 audit and frozen
  Gecko oracle pass locally. Final Windows workflow `30024467639`, job
  `89265258610`, HEAD `a318cb936dfc0ae56f71a100573e0295d050624f`: **PASS**.

T302 is closed. T303 artifact validation, T304 ProjectIO and later publication
remain deferred. T301 store-ID assignment is consumed but not changed here.
