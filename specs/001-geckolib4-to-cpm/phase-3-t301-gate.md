# Fase 3 — Gate T301

Status: **[x] PASS**

Commit-base: `2c3cf0eead8d16f697660ffca5866796824f3ea3`

Implementation branch: `feature/t301-deterministic-store-ids`

Implementation commit: `26145d7`
Integration HEAD: `4d07fe2b170622e630bef8f6a6556413e6fd0f10`

Implementation evidence in this branch:

- `CpmStoreId` enforces the JSON/JavaScript safe range (`0..2^53-1`).
- Vanilla roots resolve to reserved IDs HEAD=0, BODY=1, LEFT_ARM=2,
  RIGHT_ARM=3, LEFT_LEG=4 and RIGHT_LEG=5; ID 6 and 7–999 are not allocated.
- `CpmStoreIdAssigner` assigns every non-root BONE/CUBE/HELPER in logical
  pre-order, contiguously from 1000, with capacity preflight.
- `CpmStoreIdRegistry` and `CpmResolvedProjectionIndex` preserve order,
  reverse lookups, target resolution and immutable collections.
- `CpmStoreIdAssignmentValidator` checks ranges, coverage, contiguity,
  duplicate IDs/keys, index resolution and helper/cube separation.
- Registry/policy tests pass; full `clean check`, manifest check, S004 audit and
  frozen Gecko oracle pass locally.

Deferred: T302 writer/JSON/ZIP, T303 persisted-artifact validation, T304
ProjectIO/visual conformance, T400/T401/T402 animation projection.

Final Windows workflow: run `30011789122`, job `89221476369`, HEAD
`4d07fe2b170622e630bef8f6a6556413e6fd0f10`, conclusion **PASS**.

The manifest check, S004 audit, frozen Gecko oracle and `clean check` also
passed locally. T302 (writer), T303 (persisted-artifact validator), T304
(ProjectIO/visual conformance) and later animation projection remain deferred.
