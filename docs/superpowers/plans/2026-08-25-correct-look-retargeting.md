# Correct Look Retargeting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the Phase 1 math/IR/config gaps needed to guarantee that converted Gecko animations and CPM head/neck look compose without double rotation, snapping, hierarchy distortion or silent semantic data loss.

**Architecture:** Preserve the Gecko hierarchy under a synthetic `entity_root`, treat authored animation as the base pose, and apply player look as a semantic additive layer solved in the animated hierarchy's local space. Complete matrix/TRS decomposition and continuous ZYX extraction before any production projection/retargeting code depends on them.

**Tech Stack:** Java 17, Gradle 8.8 wrapper, JUnit 5, networknt JSON Schema 2020-12, GitHub Actions Ubuntu/Windows.

**Spec:** `docs/superpowers/specs/2026-08-25-correct-look-retargeting-design.md`

## Global Constraints

- Minecraft Java 1.20.1 / Forge target.
- GeckoLib exactly 4.4.9 for the production compatibility target.
- Gecko geometry format 1.12.0.
- CPM project V1 / pinned CPM 0.6.27 evidence remains normative for current discovery.
- Rotation order is ZYX (`Rz × Ry × Rx`) for column vectors.
- Source-authored winding must not be erased before sampling.
- Shear/non-TRS transforms must be rejected or diagnosed, never silently approximated.
- `master` must not be modified directly during implementation.

---

### Task 1: Lock the semantic contract in project SDD

**Files:**
- Modify: `docs/decisions/ADR-005-head-retargeting.md`
- Modify: `docs/animation-retargeting.md`
- Modify: `specs/001-geckolib4-to-cpm/requirements.md`
- Modify: `specs/001-geckolib4-to-cpm/acceptance-criteria.md`
- Modify: `specs/001-geckolib4-to-cpm/test-plan.md`
- Modify: `specs/001-geckolib4-to-cpm/tasks.md`

**Interfaces:**
- Consumes: accepted design spec above.
- Produces: normative definitions of structural/animation/semantic correctness, synthetic `entity_root`, and look-composition acceptance cases.

- [ ] Update ADR-005 so single-anchor + synthetic `entity_root` is the provisional production architecture and look is composed after base animation rather than replacing the authored head channel.
- [ ] Add release criteria covering idle/walk/run/jump/attack + look, ±179° crossing, influence split and deep hierarchy.
- [ ] Mark T102/T104/T500 subrequirements explicitly in `tasks.md` without falsely marking unverified work complete.
- [ ] Commit documentation changes.

### Task 2: Add failing golden tests for representable affine decomposition

**Files:**
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/math/Mat4dTest.java`
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/math/QuatdTest.java`
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/math/RotationContinuityTest.java`
- Create: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/math/TransformDecompositionTest.java`

**Interfaces:**
- Produces desired APIs: `Mat4d.decomposeTrs(double tolerance)`, `Quatd.fromRotationMatrix(Mat4d)`, `EulerAnglesZYX.fromQuaternion(Quatd, Vec3d previousDegrees)`, and continuity helpers.

- [ ] Add +90° X/Y/Z and non-commuting rotation goldens with manually expected transformed axes.
- [ ] Add TRS roundtrip for translation + arbitrary rotation + non-uniform scale.
- [ ] Add world-transform-preserving reparenting golden.
- [ ] Add explicit shear rejection case.
- [ ] Add ±179° continuity and 0°→720° source-hint preservation cases.
- [ ] Push tests alone and verify GitHub Actions fails for missing APIs/behavior.

### Task 3: Implement TRS decomposition and continuous ZYX extraction

**Files:**
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/math/Mat4d.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/math/Quatd.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/math/EulerAnglesZYX.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/math/RotationContinuity.java`

**Interfaces:**
- Consumes tests from Task 2.
- Produces matrix→TRS and quaternion/matrix→continuous ZYX required by rebake/look composition.

- [ ] Extract translation from affine matrix and scale from basis-column lengths.
- [ ] Normalize basis, verify orthogonality within tolerance, and reject zero-scale/shear/non-affine matrices.
- [ ] Correct reflection deterministically by assigning sign to one scale axis while retaining a proper rotation matrix.
- [ ] Convert normalized rotation matrix to quaternion.
- [ ] Extract principal ZYX Euler values with a gimbal branch and choose equivalent angles nearest the previous/source hint.
- [ ] Preserve winding from explicit source hint; do not claim quaternion/matrix recovery of lost turns.
- [ ] Run CI until Ubuntu/Windows math tests and formatting are green.

### Task 4: Enforce IR cube ownership consistency

**Files:**
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/diagnostics/DiagnosticCodes.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/ir/ModelIrValidator.java`
- Modify: `converter-core/src/test/java/io/github/gabriel0liv/cpmconverter/ir/HierarchyGoldenTest.java`

**Interfaces:**
- Produces: `IR_CUBE_BONE_MISMATCH` diagnostic whenever a cube listed under bone A claims `cube.bone() == B`.

- [ ] Add failing validator test for contradictory cube ownership.
- [ ] Add diagnostic code and validator check.
- [ ] Verify existing valid hierarchy still passes and CI remains green.

### Task 5: Preserve and validate look limits through semantic compilation

**Files:**
- Modify: `converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/CompiledLookConfig.java`
- Modify: `converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/MappingCompiler.java`
- Modify: `converter-config/src/main/java/io/github/gabriel0liv/cpmconverter/config/MappingValidator.java`
- Modify: `converter-config/src/test/java/io/github/gabriel0liv/cpmconverter/config/ConfigTest.java`

**Interfaces:**
- Produces: immutable deterministic `limits` in `CompiledLookConfig` and defense-in-depth finite/non-negative semantic validation.

- [ ] Add failing compile test showing `look.limits` currently disappears.
- [ ] Add invalid negative/non-finite semantic limit tests where representable by the DTO path.
- [ ] Preserve limits in deterministic key order and validate them before compilation.
- [ ] Verify fixture compiled mappings that include look limits remain deterministic.

### Task 6: Record Phase 1 status without overstating completion

**Files:**
- Modify: `specs/001-geckolib4-to-cpm/phase-1-final-gate.md`
- Modify: `specs/001-geckolib4-to-cpm/tasks.md`

**Interfaces:**
- Consumes: CI evidence from Tasks 2–5.
- Produces: an auditable gate state saying which T102/T103/T104 pieces are now green and which manual visual/T105 blockers remain.

- [ ] Record exact GitHub Actions run(s) used as evidence.
- [ ] Mark only verified subitems complete.
- [ ] Keep T200 blocked if T105/manual gate requirements remain open.
- [ ] Commit gate update.
