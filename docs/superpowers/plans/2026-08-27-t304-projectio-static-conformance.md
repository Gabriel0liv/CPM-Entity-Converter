# T304 ProjectIO Static Conformance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove that `.cpmproject` bytes emitted by the current static converter pipeline load and materialize correctly through official CPM 0.6.27 `ProjectIO`, remain deterministic on Windows/Linux, survive semantic save/reopen, and produce a human visual-review bundle without leaking CPM dependencies into production modules.

**Architecture:** Add a verification-only Gradle module `verification-projectio`. The module builds fixtures A/B/C/D through the current `GeckoGeometryParser`/`GeckoAnimationParser`/`GeckoTextureLoader` + mapping + `CpmStaticProjector` + `CpmStoreIdAllocator` + `CpmProjectWriterV1` path, validates the exact emitted bytes with T303 `GENERATED_V1`, then loads those same bytes through CPM 0.6.27 `ProjectIO` compiled from a separately checked-out pinned source tree. Stable converter-owned snapshots isolate tests from CPM internals; ProjectIO save/reopen is compared semantically, not byte-for-byte.

**Tech Stack:** Java 17, Gradle 8.8, JUnit 5, current converter modules, official CustomPlayerModels shared/editor source at commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`, Gson 2.8.5, Guava 21.0, Brigadier 1.0.18, Netty 4.1.25.Final, GitHub Actions Ubuntu/Windows.

**Spec:** `docs/superpowers/specs/2026-08-27-t304-projectio-static-conformance-design.md`, `specs/001-geckolib4-to-cpm/acceptance-criteria.md` (`AC-001`–`AC-005`, static portions of `AC-020`, `AC-022`, `AC-028`).

## Global Constraints

- CPM pin is exactly `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` / CPM `0.6.27`.
- Production modules must not depend on CPM, Minecraft, Forge, GeckoLib runtime classes, or CPM editor classes.
- `verification-projectio` is verification-only; no production module may depend on it.
- The ProjectIO harness receives the exact bytes emitted by `CpmProjectWriterV1`; it must not rewrite ZIP entries, JSON, UV, IDs, or texture bytes before official loading.
- T303 `CpmProjectValidator` with `GENERATED_V1` runs before ProjectIO for generated A/B/C/D artifacts.
- Fixture A/B/C/D source geometry declares a 32x32 texture grid and uses `texture.png`; T304 loads that texture through `GeckoTextureLoader`, preserving original PNG bytes.
- S003 M2/M3/M4/M5 are positive ProjectIO controls; M0/M1 remain negative controls according to the established S003 evidence.
- Bind tolerances stay: position `<= 1e-4` pixel, rotation `<= 1e-4°`, scale `<= 1e-6`.
- ProjectIO round-trip means structural/semantic equivalence; CPM-authored ZIP/JSON bytes need not equal converter canonical bytes.
- Current-architecture hashes must be established from current output; historical T304 hashes are not imported as truth.
- Ordinary root `check` must work without a CPM checkout. The dedicated ProjectIO CI job makes `-PcpmReferenceDir` mandatory.
- Visual evidence starts as `NOT RUN` and cannot be marked PASS by automation.
- `master` remains untouched; work stays on `agent/correct-look-retargeting-phase1`.

---

### Task 1: Verification module and pinned CPM source boundary

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `verification-projectio/build.gradle`
- Create: `verification-projectio/gradle.lockfile`
- Modify: `gradle/verification-metadata.xml`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoReferenceBoundaryTest.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoReference.java`

**Interfaces:**
- Produces: `ProjectIoReference.CPM_COMMIT`, `ProjectIoReference.CPM_VERSION`.
- Gradle property: `-PcpmReferenceDir=<CustomPlayerModels checkout or repository parent>`.
- Production build remains independent when the property is absent.

- [ ] **Step 1: Add module/repository scaffolding only.**

Update `settings.gradle` so dependency resolution keeps Maven Central and adds only Mojang artifacts from the Minecraft library repository, then include the verification module:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            url = 'https://libraries.minecraft.net'
            content { includeGroup 'com.mojang' }
        }
    }
}
rootProject.name = 'cpm-entity-converter'
include 'converter-core', 'converter-config', 'adapter-geckolib4', 'writer-cpm', 'validator-cpm', 'converter-cli', 'test-fixtures', 'verification-projectio'
```

Add `project(':verification-projectio')` to the root Spotless/check configured project list. Create `verification-projectio/build.gradle` with current converter dependencies, JUnit inherited from the root, CPM reference source-set wiring, and external compile dependencies:

```groovy
apply plugin: 'com.diffplug.spotless'

def referenceProperty = providers.gradleProperty('cpmReferenceDir')
def referenceRoot = referenceProperty.map { file(it) }
def cpmRoot = referenceRoot.map { candidate ->
    def nested = file("${candidate}/CustomPlayerModels")
    nested.isDirectory() ? nested : candidate
}
def cpmSource = cpmRoot.map { file("${it}/src/shared/java") }
def cpmResources = cpmRoot.map { file("${it}/src/shared/resources") }

sourceSets {
    main {
        java {
            srcDir 'src/main/java'
            if (referenceProperty.present) srcDir cpmSource.get()
        }
        resources {
            if (referenceProperty.present) srcDir cpmResources.get()
        }
    }
}

dependencies {
    implementation project(':converter-core')
    implementation project(':converter-config')
    implementation project(':adapter-geckolib4')
    implementation project(':writer-cpm')
    implementation project(':validator-cpm')
    implementation 'com.google.code.gson:gson:2.8.5'
    implementation 'com.google.guava:guava:21.0'
    compileOnly 'com.mojang:brigadier:1.0.18'
    compileOnly 'io.netty:netty-all:4.1.25.Final'
}

def verifyCpmReference = tasks.register('verifyCpmReference') {
    onlyIf { referenceProperty.present }
    doLast {
        def checkout = cpmRoot.get()
        def projectIo = file("${checkout}/src/shared/java/com/tom/cpm/shared/editor/project/ProjectIO.java")
        if (!projectIo.isFile()) throw new GradleException("Invalid CPM checkout: ${checkout}")
        def observed = new ByteArrayOutputStream()
        exec {
            commandLine 'git', '-C', checkout.absolutePath, 'rev-parse', 'HEAD'
            standardOutput = observed
        }
        def expected = '9272f4f9c36a2bbd6986e6da65bf7091369cb12b'
        if (observed.toString().trim() != expected) {
            throw new GradleException("CPM commit mismatch: expected ${expected}, observed ${observed.toString().trim()}")
        }
    }
}

tasks.named('compileJava') {
    dependsOn verifyCpmReference
    onlyIf { referenceProperty.present }
}
tasks.named('compileTestJava') { onlyIf { referenceProperty.present } }
tasks.named('test') { onlyIf { referenceProperty.present } }
```

- [ ] **Step 2: Write RED constant/pin test before implementing `ProjectIoReference`.**

```java
class ProjectIoReferenceBoundaryTest {
  @Test
  void pinsExactCpmVersionAndCommit() {
    assertEquals("0.6.27", ProjectIoReference.CPM_VERSION);
    assertEquals(
        "9272f4f9c36a2bbd6986e6da65bf7091369cb12b", ProjectIoReference.CPM_COMMIT);
  }
}
```

- [ ] **Step 3: Run dedicated compile/test with a pinned checkout and verify RED.**

Linux/macOS:

```bash
./gradlew :verification-projectio:test -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

Windows:

```bat
.\gradlew.bat :verification-projectio:test -PcpmReferenceDir=D:\path\to\CustomPlayerModels
```

Expected: compilation fails because `ProjectIoReference` does not exist. If it fails on dependency resolution or upstream compilation instead, fix the module boundary first; that is infrastructure RED, not the behavioral RED.

- [ ] **Step 4: Implement the minimal pin class.**

```java
public final class ProjectIoReference {
  public static final String CPM_VERSION = "0.6.27";
  public static final String CPM_COMMIT = "9272f4f9c36a2bbd6986e6da65bf7091369cb12b";

  private ProjectIoReference() {}
}
```

- [ ] **Step 5: Lock dependencies/update verification metadata and verify GREEN.**

```bash
./gradlew :verification-projectio:dependencies --write-locks -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
./gradlew --write-verification-metadata sha256 :verification-projectio:test -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
./gradlew :verification-projectio:spotlessApply :verification-projectio:test -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

Review `gradle/verification-metadata.xml` so only actually resolved artifacts are added.

- [ ] **Step 6: Commit.**

```bash
git add settings.gradle build.gradle verification-projectio gradle/verification-metadata.xml
git commit -m "test: add pinned ProjectIO verification module"
```

---

### Task 2: Current A/B/C/D fixture pipeline using production APIs

**Files:**
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/CurrentFixtureArtifact.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/CurrentFixturePipeline.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/CurrentFixturePipelineTest.java`

**Interfaces:**
- Produces: `CurrentFixtureArtifact(String fixture, CpmStaticProjectV1 project, CpmStoreIdPlan storeIds, byte[] bytes, String sha256)`.
- Produces: `CurrentFixturePipeline.generate(String fixture)` for exactly A/B/C/D fixture directory names.
- The returned `bytes` have already passed T303 `GENERATED_V1` validation.

- [ ] **Step 1: Write RED test for exact current production pipeline output.**

```java
class CurrentFixturePipelineTest {
  private final CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

  @Test
  void generatesAndValidatesAllStaticFixturesDeterministically() throws Exception {
    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact first = pipeline.generate(fixture);
      CurrentFixtureArtifact second = pipeline.generate(fixture);

      assertArrayEquals(first.bytes(), second.bytes(), fixture);
      assertEquals(first.sha256(), second.sha256(), fixture);
      assertEquals(6, first.project().roots().size(), fixture);
      assertFalse(first.storeIds().elementIds().isEmpty(), fixture);
    }
  }
}
```

- [ ] **Step 2: Run the single test and verify RED because pipeline classes do not exist.**

```bash
./gradlew :verification-projectio:test --tests '*CurrentFixturePipelineTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

- [ ] **Step 3: Implement `CurrentFixtureArtifact` with defensive byte copying.**

```java
public record CurrentFixtureArtifact(
    String fixture,
    CpmStaticProjectV1 project,
    CpmStoreIdPlan storeIds,
    byte[] bytes,
    String sha256) {
  public CurrentFixtureArtifact {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
```

- [ ] **Step 4: Implement `CurrentFixturePipeline` against current APIs only.**

Use exactly these fixture names:

```java
public static final List<String> FIXTURES =
    List.of(
        "fixture-a-humanoid",
        "fixture-b-neck",
        "fixture-c-deep-hierarchy",
        "fixture-d-quadruped");
```

For each fixture:

```java
Path directory = repoRoot().resolve("test-fixtures").resolve(fixture);
Result<ModelIR> geometry = new GeckoGeometryParser().parse(directory.resolve("geometry.geo.json"));
require(geometry, "geometry");

Result<List<AnimationClipIR>> clips =
    new GeckoAnimationParser().parse(directory.resolve("animations.animation.json"), geometry.value());
require(clips, "animations");

Result<TextureIR> texture =
    new GeckoTextureLoader().load(directory.resolve("texture.png"), 32, 32);
require(texture, "texture");

ModelIR completeModel =
    new ModelIR(
        geometry.value().source(),
        geometry.value().geometryId(),
        geometry.value().bones(),
        geometry.value().roots(),
        clips.value(),
        List.of(texture.value()),
        geometry.value().unsupportedFeatures());

Result<MappingDocumentV1> mapping = new MappingLoader().load(directory.resolve("mapping.yaml"));
require(mapping, "mapping");
Result<SemanticRigMap> compiled =
    new MappingCompiler().compile(mapping.value(), new ModelIndex(completeModel));
require(compiled, "compiled mapping");

double modelScale = compiled.value().modelScale() == null ? 1.0 : compiled.value().modelScale();
double verticalOffset =
    compiled.value().verticalOffset() == null ? 0.0 : compiled.value().verticalOffset();
CpmProjectionSettings settings =
    new CpmProjectionSettings(modelScale, verticalOffset, true, true);

Result<CpmStaticProjectV1> projected = new CpmStaticProjector().project(completeModel, settings);
require(projected, "projection");
Result<CpmStoreIdPlan> ids = new CpmStoreIdAllocator().allocate(projected.value());
require(ids, "store ids");
Result<byte[]> written = new CpmProjectWriterV1().write(projected.value(), ids.value());
require(written, "writer");

Result<CpmValidationReport> validation =
    new CpmProjectValidator().validate(written.value(), CpmValidationProfile.GENERATED_V1);
require(validation, "generated validator");
```

Hash with `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(...)`. `require(Result<?>, String)` throws `AssertionError` containing the phase and diagnostics; it must never convert a failed production `Result` into a fabricated artifact.

- [ ] **Step 5: Run GREEN and root regression gate.**

```bash
./gradlew :verification-projectio:spotlessApply :verification-projectio:test --tests '*CurrentFixturePipelineTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
./gradlew spotlessCheck clean check
```

The second command intentionally omits `cpmReferenceDir`; verification-only CPM compilation must not become a normal-build requirement.

- [ ] **Step 6: Commit.**

```bash
git add verification-projectio
git commit -m "test: generate T304 fixtures through current pipeline"
```

---

### Task 3: Official ProjectIO headless load and S003 controls

**Files:**
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/CpmHeadlessEnvironment.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoElementSnapshot.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoSnapshot.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoHarness.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoLoadConformanceTest.java`

**Interfaces:**
- Produces: `ProjectIoHarness.load(byte[] archive)` and `ProjectIoHarness.load(Path archive)`.
- Produces converter-owned snapshot records; tests outside the harness do not use CPM classes directly.
- `CpmHeadlessEnvironment` contains the verification-only stubs needed by `MinecraftObjectHolder` and `Editor`.

- [ ] **Step 1: Write RED load test.**

```java
class ProjectIoLoadConformanceTest {
  @Test
  void currentFixturesAndS003ControlsMatchOfficialProjectIo() throws Exception {
    ProjectIoHarness harness = new ProjectIoHarness();
    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      ProjectIoSnapshot snapshot = harness.load(pipeline.generate(fixture).bytes());
      assertTrue(snapshot.loaded(), fixture + ": " + snapshot.failureMessage());
      assertEquals(6, snapshot.rootCount(), fixture);
    }

    Path s003 = repoRoot().resolve("spikes/minimal-cpmproject/artifacts");
    for (String name : List.of("M2", "M3", "M4", "M5")) {
      assertTrue(harness.load(s003.resolve(name + ".cpmproject")).loaded(), name);
    }
    assertFalse(harness.load(s003.resolve("M0.cpmproject")).loaded(), "M0");
    assertFalse(harness.load(s003.resolve("M1.cpmproject")).loaded(), "M1");
  }
}
```

- [ ] **Step 2: Verify RED because official harness/snapshots do not exist.**

```bash
./gradlew :verification-projectio:test --tests '*ProjectIoLoadConformanceTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

- [ ] **Step 3: Implement a verification-only headless environment.**

Port the existing S003 `ProjectIoOracle` environment behavior into `CpmHeadlessEnvironment`, without modifying the production modules. It must:

```java
static void initialize() {
  // MinecraftObjectHolder common access: no platform features, version 1.20.1, CPM 0.6.27.
  // Item/block/entity/biome handlers return empty deterministic data.
  // MinecraftClientAccess proxy returns SkinType.DEFAULT, AWTImageIO, and executes game-thread Runnables inline.
  // AllTagManagers is initialized after clientObject is installed.
}

static Editor newEditor() {
  Editor editor = new Editor();
  // UI proxy: i18nFormat returns key; executeLater runs inline.
  editor.skinType = SkinType.DEFAULT;
  for (PlayerModelParts part : PlayerModelParts.VALUES) {
    if (part != PlayerModelParts.CUSTOM_PART) {
      editor.elements.add(new ModelElement(editor, ElementType.ROOT_PART, part));
    }
  }
  return editor;
}
```

The concrete proxy/handler methods must match the already proven S003 implementation in `spikes/minimal-cpmproject/scripts/oracle/src/main/java/spike/ProjectIoOracle.java`; do not add gameplay behavior to the stubs.

- [ ] **Step 4: Implement ProjectIO load without artifact rewriting.**

`load(byte[])` writes bytes only to a temporary file because `ProjectFile.load(File)` requires a file path:

```java
Path temporary = Files.createTempFile("t304-projectio-", ".cpmproject");
try {
  Files.write(temporary, archive);
  return load(temporary);
} finally {
  Files.deleteIfExists(temporary);
}
```

`load(Path)`:

```java
CpmHeadlessEnvironment.initialize();
ProjectFile project = new ProjectFile();
project.load(path.toFile()).join();
Editor editor = CpmHeadlessEnvironment.newEditor();
ProjectIO.loadProject(editor, project);
return ProjectIoSnapshot.fromLoadedEditor(editor);
```

Unwrap `CompletionException` / `ExecutionException` causes and return a failure snapshot with the root exception class and normalized message. A ProjectIO rejection is evidence, not a swallowed exception.

- [ ] **Step 5: Implement stable snapshot extraction.**

`ProjectIoElementSnapshot` records at least:

```java
record ProjectIoElementSnapshot(
    String path,
    String parentPath,
    String name,
    String type,
    long storeId,
    boolean texture,
    int textureSize,
    int u,
    int v,
    Vec3Snapshot position,
    Vec3Snapshot rotation,
    Vec3Snapshot scale,
    Vec3Snapshot meshScale,
    boolean hasFaceUv) {}
```

Traverse `editor.elements` recursively in source order. Root display names must use stable CPM type data (`VanillaModelPart.getName()` when available) rather than localized UI text. The snapshot owns only primitives/strings/records.

- [ ] **Step 6: Run GREEN and commit.**

```bash
./gradlew :verification-projectio:spotlessApply :verification-projectio:test --tests '*ProjectIoLoadConformanceTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
git add verification-projectio
git commit -m "test: load current CPM artifacts with official ProjectIO"
```

---

### Task 4: Persisted IDs, hierarchy, bind transforms and UV/texture materialization

**Files:**
- Modify: `ProjectIoElementSnapshot.java`
- Modify: `ProjectIoSnapshot.java`
- Modify: `ProjectIoHarness.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ExpectedStaticSnapshot.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoIdentityConformanceTest.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoHierarchyBindConformanceTest.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoUvTextureConformanceTest.java`

**Interfaces:**
- Produces: `ExpectedStaticSnapshot.from(CpmStaticProjectV1, CpmStoreIdPlan)`.
- Compares official loaded state to the converter graph, not merely to weak presence predicates.

- [ ] **Step 1: Add RED identity test.**

For every generated fixture, flatten expected generated elements in canonical root/preorder order and compare expected persisted IDs to loaded IDs:

```java
assertEquals(expected.generatedStoreIds(), loaded.generatedStoreIds(), fixture);
assertEquals(expected.generatedPaths(), loaded.generatedPaths(), fixture);
assertTrue(loaded.generatedStoreIds().stream().allMatch(id -> id >= 1000 && id <= CpmStoreIdAllocator.MAX_SAFE_ID));
assertEquals(loaded.generatedStoreIds().size(), new HashSet<>(loaded.generatedStoreIds()).size());
```

Also keep S003 identity controls:

```java
assertTrue(harness.load(m3).storeIds().contains(1000L));
ProjectIoSnapshot m5 = harness.load(m5Path);
assertTrue(m5.storeIds().contains(1000L));
assertTrue(m5.animationReferenceCount() > 0);
```

- [ ] **Step 2: Run identity test and verify RED on missing snapshot fields/extraction.**

- [ ] **Step 3: Implement ID/reference extraction.**

Collect every positive `ModelElement.storeID` recursively. For animation reference count, keep reflection isolated inside `ProjectIoHarness`: access editor animation `frames`, frame `components`, and count component-map entries. Return `-1` only if the fixed upstream structure unexpectedly changes; tests require a positive count for M5, so such a change is visible.

- [ ] **Step 4: Add RED hierarchy/bind test.**

For generated A/B/C/D require the loaded graph contains the single-anchor path and exact expected parent paths:

```java
assertTrue(loaded.paths().stream().anyMatch(path -> path.contains("body/entity_root")), fixture);
assertEquals(expected.parentByGeneratedPath(), loaded.parentByGeneratedPath(), fixture);
```

Compare every expected generated element local transform using named tolerances:

```java
assertVec(expected.position(), actual.position(), 1e-4);
assertVec(expected.rotationDegrees(), actual.rotation(), 1e-4);
assertVec(expected.scale(), actual.meshScale(), 1e-6);
```

Do not compare CPM `getScale()` because `ModelElement.getScale()` is animation-facing identity; the persisted static element scale is represented by the loaded `meshScale`/serialized fields established by CPM V1.

- [ ] **Step 5: Implement `ExpectedStaticSnapshot` from the current project graph/store-ID plan.**

Walk `CpmStaticProjectV1.roots()` in `CpmVanillaPart.values()` order and children recursively. Expected generated paths derive from actual converter names; expected IDs come only from `CpmStoreIdPlan.elementId(element.key())`. Do not parse converter output JSON to manufacture the expected graph.

- [ ] **Step 6: Add RED UV/texture test stronger than the historical T304 test.**

For each fixture assert:

```java
assertTrue(loaded.elements().stream().anyMatch(ProjectIoElementSnapshot::texture), fixture);
assertTrue(loaded.elements().stream().filter(ProjectIoElementSnapshot::texture).allMatch(e -> e.textureSize() > 0), fixture);
assertEquals(expected.boxUvOriginsByPath(), loaded.boxUvOriginsByGeneratedPath(), fixture);
assertEquals(expected.perFaceUvPresenceByPath(), loaded.perFaceUvPresenceByGeneratedPath(), fixture);
```

Fixture C must exercise the per-face path because its `accessory` cube has explicit six-face UV data.

- [ ] **Step 7: Implement UV/face extraction.**

Read public `ModelElement.texture`, `textureSize`, `u`, `v`, and `faceUV`. `hasFaceUv = element.faceUV != null`. For fixture C, extract the six face rectangles/rotations from CPM `PerFaceUV` into converter-owned face snapshots and compare to expected serialized CPM V1 face data. Keep any reflection required for fixed upstream face internals in one private harness method and fail loudly if a required field cannot be observed.

- [ ] **Step 8: Run all three GREEN tests and commit.**

```bash
./gradlew :verification-projectio:spotlessApply :verification-projectio:test --tests '*ProjectIoIdentityConformanceTest' --tests '*ProjectIoHierarchyBindConformanceTest' --tests '*ProjectIoUvTextureConformanceTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
git add verification-projectio
git commit -m "test: verify ProjectIO static materialization"
```

---

### Task 5: Official save/reopen semantic round-trip

**Files:**
- Modify: `ProjectIoHarness.java`
- Modify: `ProjectIoSnapshot.java`
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoRoundTripResult.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/ProjectIoRoundTripTest.java`

**Interfaces:**
- Produces: `ProjectIoHarness.roundTrip(byte[] archive)`.
- Result contains `before`, `after`, and explicit status; T304 requires `PASS`, not the historical permissive `NOT_AVAILABLE` state.

- [ ] **Step 1: Write RED round-trip test.**

```java
@Test
void officialSaveAndReopenPreservesStaticSemantics() throws Exception {
  for (String fixture : CurrentFixturePipeline.FIXTURES) {
    CurrentFixtureArtifact artifact = pipeline.generate(fixture);
    ProjectIoRoundTripResult result = harness.roundTrip(artifact.bytes());

    assertEquals(ProjectIoRoundTripResult.Status.PASS, result.status(), fixture + ": " + result.message());
    assertEquals(result.before().generatedStoreIds(), result.after().generatedStoreIds(), fixture);
    assertEquals(result.before().parentByGeneratedPath(), result.after().parentByGeneratedPath(), fixture);
    assertEquals(result.before().boxUvOriginsByGeneratedPath(), result.after().boxUvOriginsByGeneratedPath(), fixture);
  }
}
```

- [ ] **Step 2: Verify RED because round-trip API is absent.**

- [ ] **Step 3: Implement official save/reopen.**

Load into an `Editor`, then:

```java
Path temporary = Files.createTempFile("t304-roundtrip-", ".cpmproject");
try {
  ProjectFile saved = new ProjectFile();
  ProjectIO.saveProject(editor, saved);
  saved.save(temporary.toFile()).join();
  Editor reopened = loadEditor(temporary);
  return ProjectIoRoundTripResult.pass(snapshot(editor), snapshot(reopened));
} finally {
  Files.deleteIfExists(temporary);
}
```

Any exception becomes `Status.FAIL` with normalized root-cause type/message. Do not classify unavailable reflection/APIs as acceptable: the CPM version is pinned, so an inability to save/reopen is a T304 failure.

- [ ] **Step 4: Verify GREEN and commit.**

```bash
./gradlew :verification-projectio:spotlessApply :verification-projectio:test --tests '*ProjectIoRoundTripTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
git add verification-projectio
git commit -m "test: require ProjectIO static round trip"
```

---

### Task 6: Current-architecture deterministic hashes and cross-platform CI

**Files:**
- Create: `verification-projectio/src/main/java/io/github/gabriel0liv/cpmconverter/verification/T304EvidenceWriter.java`
- Create: `verification-projectio/src/test/java/io/github/gabriel0liv/cpmconverter/verification/CpmCrossPlatformGoldenTest.java`
- Create: `verification-projectio/expected-artifact-hashes.properties`
- Modify: `verification-projectio/build.gradle`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Gradle tasks: `t304ProjectIoConformance`, `t304EvidenceBundle`.
- Stable committed manifest: four current-architecture SHA-256 values only.
- CI artifact: `t304-evidence-<os>` containing reports and exact generated artifacts.

- [ ] **Step 1: Write RED golden test with no committed hashes yet.**

```java
@Test
void emittedBytesMatchCurrentArchitectureGoldens() throws Exception {
  Properties expected = loadExpectedHashes();
  assertEquals(Set.copyOf(CurrentFixturePipeline.FIXTURES), expected.stringPropertyNames());
  for (String fixture : CurrentFixturePipeline.FIXTURES) {
    assertEquals(expected.getProperty(fixture), pipeline.generate(fixture).sha256(), fixture);
  }
}
```

Expected RED: manifest absent or missing all four fixture keys.

- [ ] **Step 2: Add evidence writer before freezing hashes.**

`T304EvidenceWriter` generates A/B/C/D twice, requires byte identity, loads/round-trips each through ProjectIO, then writes under `build/t304/`:

```text
build/t304/
  artifacts/
    fixture-a-humanoid.cpmproject
    fixture-b-neck.cpmproject
    fixture-c-deep-hierarchy.cpmproject
    fixture-d-quadruped.cpmproject
    manifest.json
  projectio-report.json
  manual-evidence/
    README.md
    checklist.md
    manifest.json
    artifacts/<same four exact files>
    projectio-report.json
    screenshots/fixture-a-humanoid/
    screenshots/fixture-b-neck/
    screenshots/fixture-c-deep-hierarchy/
    screenshots/fixture-d-quadruped/
    round-trip/
```

The JSON report includes converter commit, CPM version/commit, fixture hash, validator status, ProjectIO load status, round-trip status, root/element/storeID counts, and stable structural snapshots. It contains no current timestamps in deterministic machine-comparison fields.

Manual files must literally state `visualValidation: NOT RUN`. No code path writes `PASS` into visual fields.

- [ ] **Step 3: Run one pinned reference generation, inspect results, then freeze the four current hashes.**

```bash
./gradlew :verification-projectio:t304EvidenceBundle -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

Before committing hashes, confirm all four generated artifacts pass `GENERATED_V1`, ProjectIO load, materialization assertions, and round-trip. Then write exactly:

```properties
fixture-a-humanoid=<observed-current-hash>
fixture-b-neck=<observed-current-hash>
fixture-c-deep-hierarchy=<observed-current-hash>
fixture-d-quadruped=<observed-current-hash>
```

The values are the observed current-branch hashes from this step, not historical branch values.

- [ ] **Step 4: Run golden test GREEN on the same platform.**

```bash
./gradlew :verification-projectio:test --tests '*CpmCrossPlatformGoldenTest' -PcpmReferenceDir=/absolute/path/to/CustomPlayerModels
```

- [ ] **Step 5: Add dedicated Ubuntu/Windows ProjectIO CI job.**

Keep existing `check` unchanged. Add:

```yaml
  projectio-conformance:
    strategy:
      matrix:
        os: [windows-latest, ubuntu-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {distribution: temurin, java-version: '17'}
      - uses: gradle/actions/setup-gradle@v4
      - uses: actions/checkout@v4
        with:
          repository: tom5454/CustomPlayerModels
          ref: 9272f4f9c36a2bbd6986e6da65bf7091369cb12b
          path: cpm-reference
      - if: runner.os == 'Linux'
        run: bash ./gradlew :verification-projectio:spotlessCheck :verification-projectio:test :verification-projectio:t304EvidenceBundle -PcpmReferenceDir="$GITHUB_WORKSPACE/cpm-reference"
      - if: runner.os == 'Windows'
        run: .\gradlew.bat :verification-projectio:spotlessCheck :verification-projectio:test :verification-projectio:t304EvidenceBundle "-PcpmReferenceDir=$env:GITHUB_WORKSPACE\cpm-reference"
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: t304-evidence-${{ matrix.os }}
          path: build/t304
          if-no-files-found: warn
```

The module's `verifyCpmReference` still executes `git rev-parse HEAD`; checkout `ref` alone is not the pin proof.

- [ ] **Step 6: Commit and trigger CI.**

```bash
git add verification-projectio .github/workflows/ci.yml
git commit -m "test: gate ProjectIO conformance across platforms"
git push
```

- [ ] **Step 7: Verify terminal GREEN on both jobs before continuing.**

Required evidence:

- normal `check (ubuntu-latest)` GREEN;
- normal `check (windows-latest)` GREEN;
- `projectio-conformance (ubuntu-latest)` GREEN;
- `projectio-conformance (windows-latest)` GREEN;
- uploaded evidence bundle for both OSes;
- same four hashes on both OSes.

If a hash differs, do not update the golden separately per OS. Investigate until the current writer emits identical bytes or the gate fails explicitly.

---

### Task 7: T304 gate documentation and manual visual handoff

**Files:**
- Create: `specs/001-geckolib4-to-cpm/phase-3-t304-gate.md`
- Create: `specs/001-geckolib4-to-cpm/phase-3-t304-manual-checklist.md`
- Modify: `specs/001-geckolib4-to-cpm/tasks.md`
- Modify: `specs/001-geckolib4-to-cpm/test-plan.md`
- Modify: `specs/001-geckolib4-to-cpm/traceability.md`

**Interfaces:**
- Produces an auditable automated T304 PASS record while keeping task status `[~]` until human CPM Editor evidence exists.

- [ ] **Step 1: Write gate document from the exact terminal-green run.**

Record:

```markdown
# Gate T304

Status: **[~] AUTOMATED PASS — MANUAL VISUAL PENDING**

CPM: `0.6.27`, commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`
CI run: `<exact terminal-green run id>`

Automated:
- A/B/C/D generated by current production pipeline: PASS
- T303 GENERATED_V1 validation: PASS
- official ProjectIO load: PASS
- S003 M2/M3/M4/M5 positive controls: PASS
- S003 M0/M1 negative controls: PASS
- IDs/references: PASS
- hierarchy/bind transforms: PASS
- texture/UV including fixture C per-face UV: PASS
- ProjectIO save/reopen semantic round-trip: PASS
- deterministic A/B/C/D SHA-256 identical on Ubuntu/Windows: PASS

Manual visual validation: NOT RUN
```

Insert the four exact current hashes from `expected-artifact-hashes.properties`.

- [ ] **Step 2: Write manual checklist with immutable hash binding.**

Each A/B/C/D row contains its frozen SHA-256 and starts with `NOT RUN` for Open, Texture/UV, Hierarchy/bind, Save/reopen, screenshot path and observations. Procedure:

1. use the exact CI artifact or locally regenerated artifact whose SHA-256 matches the gate;
2. open in CPM Editor 0.6.27;
3. verify roots/hierarchy/names, texture/UV, pivots/static orientation;
4. Save As to a temporary copy, close, reopen, repeat;
5. record literal warnings/errors and attach screenshots;
6. never replace source artifact during the manual session.

- [ ] **Step 3: Update traceability/test plan and keep T304 `[~]`.**

`tasks.md` line remains:

```markdown
- [~] T304 conformidade `ProjectIO` e visual estático (AC-001–005) — gate automatizado ProjectIO/round-trip/determinismo Ubuntu+Windows verde no run <id>; checklist visual CPM 0.6.27 pendente.
```

Do **not** mark `[x]` until the manual checklist is actually performed for the exact hashed artifacts.

- [ ] **Step 4: Run final repository gate after documentation changes.**

```bash
./gradlew spotlessCheck clean check
```

Then push the documentation commit and verify the normal Ubuntu/Windows CI is green for that exact documentation HEAD. The previous ProjectIO run remains the automated T304 evidence because documentation does not alter generated artifact bytes; if any executable/build/fixture file changes after the ProjectIO run, rerun the ProjectIO matrix instead.

- [ ] **Step 5: Commit.**

```bash
git add specs/001-geckolib4-to-cpm
git commit -m "docs: record automated T304 ProjectIO gate"
git push
```

---

## Final verification checklist

Before stating the automated portion of T304 is complete, verify all of the following from fresh evidence:

- [ ] `./gradlew spotlessCheck clean check` passes without `cpmReferenceDir`.
- [ ] Pinned CPM checkout identity equals `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`.
- [ ] `:verification-projectio:test` passes against the pin.
- [ ] A/B/C/D pass T303 `GENERATED_V1` before ProjectIO.
- [ ] A/B/C/D load through official ProjectIO.
- [ ] M2/M3/M4/M5 load; M0/M1 remain negative controls.
- [ ] IDs, references, hierarchy, bind transforms, texture/UV and fixture C per-face UV assertions pass.
- [ ] ProjectIO save/reopen semantic round-trip passes for A/B/C/D.
- [ ] A/B/C/D outputs are byte-identical across two generations.
- [ ] The four SHA-256 values are identical on Ubuntu and Windows.
- [ ] Normal CI and dedicated ProjectIO CI are terminal green.
- [ ] Evidence bundle contains the exact passing hashes and `visualValidation: NOT RUN`.
- [ ] T304 remains `[~]` until the human CPM Editor checklist is completed.
