# CPM V1 Layered Validator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement T303: validate generated and pre-existing CPM V1 `.cpmproject` archives in independent container, schema, graph/UV/reference, animation-frame, and deterministic-output layers.

**Architecture:** `validator-cpm` stays runtime-independent from CPM/Minecraft. `CpmProjectValidator` accepts archive bytes and a validation profile; an archive reader parses ZIP/JSON into immutable validation data, then focused validators accumulate stable diagnostics. `EXISTING_V1` accepts loader-compatible noncanonical projects; `GENERATED_V1` additionally enforces this converter's deterministic ZIP/JSON/root/storeID conventions so FR-022 does not make FR-028 reject otherwise valid CPM V1 projects.

**Tech Stack:** Java 17, Gradle 8.8, JUnit 5, Jackson Databind 2.17.2, `java.util.zip`, existing `converter-core` diagnostics, GitHub Actions Ubuntu/Windows.

**Spec:** `specs/001-geckolib4-to-cpm/requirements.md` (`FR-022`, `FR-028`), `acceptance-criteria.md` (`AC-001`–`AC-004`, `AC-008`), `docs/decisions/ADR-003-cpm-output-strategy.md`.

## Global Constraints

- Runtime must not depend on CPM, Minecraft, Forge, GeckoLib, or Blockbench.
- `config.json` and `version: 1` are required; `elements` must exist as a list, per S003/ProjectIO evidence.
- Existing CPM V1 validation must not require converter-specific canonical root order or sequential generated IDs.
- Generated-output validation must reject duplicate IDs, dangling animation refs, noncanonical generated `storeID` preorder, fractional/invalid UV, malformed frames, nondeterministic entry order/timestamp/method, and noncanonical JSON.
- IDs must be positive and `<= 2^53-1`; reserved vanilla animation refs `0..6` remain legal.
- Diagnostics are stable and documented; no recognized invalid feature disappears silently.
- `master` remains untouched; implementation stays on `agent/correct-look-retargeting-phase1`.

---

### Task 1: Container/schema validator and public API

**Files:**
- Modify: `validator-cpm/build.gradle`
- Create: `validator-cpm/src/main/java/io/github/gabriel0liv/cpmconverter/cpm/validation/CpmValidationProfile.java`
- Create: `validator-cpm/src/main/java/io/github/gabriel0liv/cpmconverter/cpm/validation/CpmValidationReport.java`
- Create: `validator-cpm/src/main/java/io/github/gabriel0liv/cpmconverter/cpm/validation/CpmProjectValidator.java`
- Create: `validator-cpm/src/test/java/io/github/gabriel0liv/cpmconverter/cpm/validation/CpmProjectValidatorTest.java`
- Modify: `converter-core/src/main/java/io/github/gabriel0liv/cpmconverter/diagnostics/DiagnosticCodes.java`
- Modify: `specs/001-geckolib4-to-cpm/diagnostics.md`

**Interfaces:**
- Produces: `Result<CpmValidationReport> CpmProjectValidator.validate(byte[] archive, CpmValidationProfile profile)`.
- Profiles: `EXISTING_V1`, `GENERATED_V1`.
- Report fields: `entryCount`, `elementCount`, `animationCount`, `storeIdCount`.

- [ ] **Step 1: Write RED tests for container/schema behavior**

```java
assertTrue(validator.validate(validM2LikeArchive(), EXISTING_V1).success());
assertHasCode(validator.validate(notZip(), EXISTING_V1), CPM_ZIP_INVALID);
assertHasCode(validator.validate(zipWithoutConfig(), EXISTING_V1), CPM_CONFIG_INVALID);
assertHasCode(validator.validate(zipOf("config.json", "{\"version\":1}"), EXISTING_V1), CPM_CONFIG_INVALID);
assertHasCode(validator.validate(zipOf("config.json", "{\"version\":2,\"elements\":[]}"), EXISTING_V1), INPUT_UNSUPPORTED_VERSION);
```

- [ ] **Step 2: Run CI and verify RED is missing validator API/codes, not formatting.**
- [ ] **Step 3: Implement ZIP parsing, duplicate/path traversal rejection, strict JSON parse, version/elements checks, and report counts.**
- [ ] **Step 4: Run CI until Task 1 tests are GREEN on Ubuntu/Windows.**

### Task 2: Graph, storeID and UV layers

**Files:**
- Modify: `CpmProjectValidator.java`
- Modify: `CpmProjectValidatorTest.java`
- Modify: `DiagnosticCodes.java`
- Modify: `diagnostics.md`

**Interfaces:**
- Consumes parsed `elements` tree.
- Produces stable errors `CPM_DUPLICATE_STORE_ID`, `CPM_INVALID_ROOT`, `CPM_UV_INVALID`, `CPM_VALIDATION_FAILED`.

- [ ] **Step 1: Add RED tests for duplicate/out-of-range IDs, unknown/duplicate roots, fractional CPM UV endpoints, missing texture for textured cubes, and generated preorder IDs.**

```java
assertHasCode(validate(existingWithDuplicate1000()), CPM_DUPLICATE_STORE_ID);
assertHasCode(validate(existingWithStoreId(9007199254740992L)), CPM_STORE_ID_RANGE);
assertHasCode(validate(existingWithUnknownRoot("wing")), CPM_INVALID_ROOT);
assertHasCode(validate(existingWithFractionalUv()), CPM_UV_INVALID);
assertTrue(validate(existingWithNonSequentialIds(), EXISTING_V1).success());
assertHasCode(validate(existingWithNonSequentialIds(), GENERATED_V1), CPM_VALIDATION_FAILED);
```

- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement recursive element walk, exact integer UV checks, known-root uniqueness, texture consistency, and generated preorder `1000..N` enforcement.**
- [ ] **Step 4: Verify GREEN.**

### Task 3: Animation/reference and deterministic-output layers

**Files:**
- Modify: `CpmProjectValidator.java`
- Modify: `CpmProjectValidatorTest.java`
- Modify: `DiagnosticCodes.java`
- Modify: `diagnostics.md`

**Interfaces:**
- Consumes `animations/*.json` and collected element IDs.
- Produces `CPM_DANGLING_ANIMATION_REF`, `CPM_FRAME_INVALID`, `CPM_VALIDATION_FAILED`.

- [ ] **Step 1: Add RED tests for dangling refs, malformed frames, invalid loop/interpolator pairs, valid reserved refs `0..6`, and generated archive determinism.**

```java
assertHasCode(validate(animationRef(9999)), CPM_DANGLING_ANIMATION_REF);
assertTrue(validate(animationRef(3)).success());
assertHasCode(validate(animationWithoutFrames()), CPM_FRAME_INVALID);
assertHasCode(validate(loopInterpolatorMismatch()), CPM_FRAME_INVALID);
assertHasCode(validateGenerated(deflatedOrTimestampedArchive()), CPM_VALIDATION_FAILED);
assertHasCode(validateGenerated(nonCanonicalConfigJson()), CPM_VALIDATION_FAILED);
```

- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Validate frame/component shape, finite transforms, refs, duration/loop/interpolator consistency; for `GENERATED_V1`, enforce lexical entries, `STORED`, timestamp 1980-01-01, LF canonical JSON, canonical converter root order and sequential generated IDs.**
- [ ] **Step 4: Verify full module GREEN.**

### Task 4: Final T303 gate

**Files:**
- Modify: `specs/001-geckolib4-to-cpm/tasks.md`

**Interfaces:**
- Produces auditable T303 closure and releases T304.

- [ ] **Step 1: Run the full CI gate: `spotlessCheck clean check`, reproducibility, fixture manifest, Gecko audit on Ubuntu/Windows.**
- [ ] **Step 2: Confirm `EXISTING_V1` accepts S003-compatible M2/M3/M4/M5 shapes while `GENERATED_V1` enforces converter determinism.**
- [ ] **Step 3: Mark T303 `[x]` with exact run ID and T304 `[~]` only after terminal green evidence.**
