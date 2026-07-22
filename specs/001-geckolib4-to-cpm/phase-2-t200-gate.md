# Phase 2 T200 Gate

Date: 2026-07-22
Commit base: 7ff8089b81fea97d5424549242f3a59f3eda6956
Implementation HEAD: 1bd8f23200c9b3c10b598b19300fb8b485b43c2d
Gate record commit: pending (integrator)
Independent review: review/t200-final-acceptance — FAIL pending final CI
Workflow: unavailable (repository has no configured `origin` remote in this environment)
Workflow HEAD: not published
Ubuntu: not executed for this HEAD
Windows: not executed for this HEAD

## Evidence

The T200 boundary parses Bedrock geometry `1.12.0`, selects identifiers exactly,
builds deterministic bones/roots/parents/children and cubes, preserves local
bind transforms, source order, provenance pointers and raw UV. The parser now
validates scalar types, parser-local limits and bidirectional parent/child
consistency. `ParsedGeometry` remains the explicit boundary to T201.

Four geometry-only fixture snapshots are compared as complete JSON trees by
`GeometryFixtureTest`. Independent translation and quaternion goldens cover the
specified examples. The eight parser limits are exercised through real parser
calls with limit context and locations. Recognized unsupported features retain
diagnostics and `FeatureOccurrence`. Manifest and GeckoLib A–D oracle checks
pass locally with 41/41 assertions and clips A idle/walk, B idle/walk, C idle,
D walk. A fresh uncached `clean check` passes locally.

## Frozen checklist status

Snapshot equality: PASS
Mathematical goldens: PASS
Eight parser limits: PASS locally
Features: PASS locally
Invalid types: PASS for covered scalar/vector cases; broader hostile matrix deferred
Path smoke: PASS for normalized separators, temporary Unicode/space paths and pointer stability
Validator: PASS for graph, reachability, ownership, provenance and isolated regressions
Manifest: PASS
Oracle: PASS (41/41)

The gate is not released because this environment cannot publish or observe the
required Ubuntu and Windows workflow for `1bd8f23200c9b3c10b598b19300fb8b485b43c2d`.

Deferred to T201: UV/PNG resolution and static ModelIR assembly.
Deferred to T202: animations, clips, tracks and keyframes.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204/T700: hostile/fuzz matrix, differential oracle and broader filesystem/locale coverage.

T200 decision: **[~] partial; CI publication and independent acceptance remain**.
