# Fase 3 — Gate T301

Status: **[~] em andamento**

Commit-base: `2c3cf0eead8d16f697660ffca5866796824f3ea3`

Implementation branch: `feature/t301-deterministic-store-ids`

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

Windows CI: pending for the integrated T301 HEAD.
