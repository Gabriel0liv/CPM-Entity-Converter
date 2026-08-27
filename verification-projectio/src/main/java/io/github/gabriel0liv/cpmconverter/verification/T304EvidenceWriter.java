package io.github.gabriel0liv.cpmconverter.verification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes deterministic automated and manual-handoff evidence for T304. */
public final class T304EvidenceWriter {
  private static final Gson JSON =
      new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

  private T304EvidenceWriter() {}

  public static void main(String[] args) throws Exception {
    Path repository = repoRoot();
    Path output = repository.resolve("build/t304");
    recreateDirectory(output);

    CurrentFixturePipeline pipeline = new CurrentFixturePipeline();
    ProjectIoHarness harness = new ProjectIoHarness();
    String converterCommit = gitHead(repository);

    Path artifacts = output.resolve("artifacts");
    Path manual = output.resolve("manual-evidence");
    Path manualArtifacts = manual.resolve("artifacts");
    Files.createDirectories(artifacts);
    Files.createDirectories(manualArtifacts);
    Files.createDirectories(manual.resolve("round-trip"));

    LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
    List<Map<String, Object>> fixtureReports = new ArrayList<>();

    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      CurrentFixtureArtifact first = pipeline.generate(fixture);
      CurrentFixtureArtifact second = pipeline.generate(fixture);
      if (!Arrays.equals(first.bytes(), second.bytes())) {
        throw new IllegalStateException(fixture + " is not byte-identical across two generations");
      }
      if (!first.sha256().equals(second.sha256())) {
        throw new IllegalStateException(fixture + " SHA-256 differs across two generations");
      }

      ProjectIoSnapshot loaded = harness.load(first.bytes());
      if (!loaded.loaded()) {
        throw new IllegalStateException(
            fixture
                + " failed official ProjectIO load: "
                + loaded.failureType()
                + ": "
                + loaded.failureMessage());
      }

      ProjectIoRoundTripResult roundTrip = harness.roundTrip(first.bytes());
      requireRoundTrip(fixture, roundTrip);

      String fileName = fixture + ".cpmproject";
      Path artifactPath = artifacts.resolve(fileName);
      Files.write(artifactPath, first.bytes());
      Files.copy(
          artifactPath, manualArtifacts.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

      hashes.put(fixture, first.sha256());
      fixtureReports.add(fixtureReport(fixture, first, loaded, roundTrip));
      Files.createDirectories(manual.resolve("screenshots").resolve(fixture));
    }

    byte[] manifest = jsonBytes(artifactManifest(converterCommit, hashes));
    Files.write(artifacts.resolve("manifest.json"), manifest);

    byte[] report = jsonBytes(projectIoReport(converterCommit, fixtureReports));
    Files.write(output.resolve("projectio-report.json"), report);
    Files.write(manual.resolve("projectio-report.json"), report);

    Files.write(
        manual.resolve("manifest.json"), jsonBytes(manualManifest(converterCommit, hashes)));
    Files.writeString(manual.resolve("README.md"), manualReadme(), StandardCharsets.UTF_8);
    Files.writeString(
        manual.resolve("checklist.md"), manualChecklist(hashes), StandardCharsets.UTF_8);
  }

  private static Map<String, Object> artifactManifest(
      String converterCommit, Map<String, String> hashes) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("converterCommit", converterCommit);
    result.put("cpmVersion", ProjectIoReference.CPM_VERSION);
    result.put("cpmCommit", ProjectIoReference.CPM_COMMIT);
    result.put("fixtures", new LinkedHashMap<>(hashes));
    return result;
  }

  private static Map<String, Object> manualManifest(
      String converterCommit, Map<String, String> hashes) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("visualValidation", "NOT RUN");
    result.put("converterCommit", converterCommit);
    result.put("cpmVersion", ProjectIoReference.CPM_VERSION);
    result.put("cpmCommit", ProjectIoReference.CPM_COMMIT);
    result.put("fixtures", new LinkedHashMap<>(hashes));
    return result;
  }

  private static Map<String, Object> projectIoReport(
      String converterCommit, List<Map<String, Object>> fixtures) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("converterCommit", converterCommit);
    result.put("cpmVersion", ProjectIoReference.CPM_VERSION);
    result.put("cpmCommit", ProjectIoReference.CPM_COMMIT);
    result.put("visualValidation", "NOT RUN");
    result.put("fixtures", fixtures);
    return result;
  }

  private static Map<String, Object> fixtureReport(
      String fixture,
      CurrentFixtureArtifact artifact,
      ProjectIoSnapshot loaded,
      ProjectIoRoundTripResult roundTrip) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("fixture", fixture);
    result.put("sha256", artifact.sha256());
    result.put("generatedValidator", "PASS");
    result.put("projectIoLoad", loaded.loaded() ? "PASS" : "FAIL");
    result.put("roundTrip", roundTrip.status().name());
    result.put("rootCount", loaded.rootCount());
    result.put("elementCount", loaded.elements().size());
    result.put("storeIdCount", loaded.storeIds().size());
    result.put("generatedStoreIds", loaded.generatedStoreIds());
    result.put("generatedPaths", loaded.generatedPaths());
    result.put("parentByGeneratedPath", loaded.parentByGeneratedPath());
    result.put("boxUvOriginsByGeneratedPath", uvOrigins(loaded.boxUvOriginsByGeneratedPath()));
    result.put("perFaceUvPresenceByGeneratedPath", loaded.perFaceUvPresenceByGeneratedPath());
    result.put("perFaceUvByGeneratedPath", faceUvs(loaded.perFaceUvByGeneratedPath()));
    return result;
  }

  private static Map<String, Object> uvOrigins(Map<String, UvOriginSnapshot> values) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    values.forEach(
        (path, uv) -> {
          LinkedHashMap<String, Object> value = new LinkedHashMap<>();
          value.put("u", uv.u());
          value.put("v", uv.v());
          result.put(path, value);
        });
    return result;
  }

  private static Map<String, Object> faceUvs(Map<String, Map<String, FaceUvSnapshot>> values) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    values.forEach(
        (path, faces) -> {
          LinkedHashMap<String, Object> serializedFaces = new LinkedHashMap<>();
          faces.forEach(
              (name, face) -> {
                LinkedHashMap<String, Object> value = new LinkedHashMap<>();
                value.put("sx", face.sx());
                value.put("sy", face.sy());
                value.put("ex", face.ex());
                value.put("ey", face.ey());
                value.put("rotation", face.rotation());
                value.put("autoUv", face.autoUv());
                serializedFaces.put(name, value);
              });
          result.put(path, serializedFaces);
        });
    return result;
  }

  private static void requireRoundTrip(String fixture, ProjectIoRoundTripResult result) {
    if (result.status() != ProjectIoRoundTripResult.Status.PASS) {
      throw new IllegalStateException(
          fixture + " failed ProjectIO round trip: " + result.message());
    }
    if (!result.before().generatedStoreIds().equals(result.after().generatedStoreIds())) {
      throw new IllegalStateException(fixture + " changed generated store IDs after round trip");
    }
    if (!result.before().parentByGeneratedPath().equals(result.after().parentByGeneratedPath())) {
      throw new IllegalStateException(fixture + " changed hierarchy after round trip");
    }
    if (!result
        .before()
        .boxUvOriginsByGeneratedPath()
        .equals(result.after().boxUvOriginsByGeneratedPath())) {
      throw new IllegalStateException(fixture + " changed box UV origins after round trip");
    }
  }

  private static byte[] jsonBytes(Object value) {
    String text = JSON.toJson(value).replace("\r\n", "\n") + "\n";
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private static String manualReadme() {
    return "# T304 manual visual evidence\n\n"
        + "visualValidation: NOT RUN\n\n"
        + "Use the exact `.cpmproject` files in `artifacts/` and CPM Editor 0.6.27. "
        + "Do not replace the source artifact during the session. Record screenshots, warnings, "
        + "save/reopen observations, and verify the SHA-256 values against `manifest.json`.\n";
  }

  private static String manualChecklist(Map<String, String> hashes) {
    StringBuilder text =
        new StringBuilder(
            "# T304 CPM Editor checklist\n\nvisualValidation: NOT RUN\n\n"
                + "| Fixture | SHA-256 | Open | Texture/UV | Hierarchy/bind | Save/reopen | Observations |\n"
                + "| --- | --- | --- | --- | --- | --- | --- |\n");
    hashes.forEach(
        (fixture, hash) ->
            text.append("| ")
                .append(fixture)
                .append(" | `")
                .append(hash)
                .append("` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | |\n"));
    text.append(
        "\nProcedure: open the exact artifact in CPM Editor 0.6.27, verify roots/hierarchy/names, "
            + "texture/UV and static pivots/orientation, Save As to a temporary copy, close/reopen, "
            + "repeat the checks, then record literal warnings/errors and screenshots.\n");
    return text.toString();
  }

  private static String gitHead(Path repository) throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder("git", "-C", repository.toString(), "rev-parse", "HEAD")
            .redirectErrorStream(true)
            .start();
    String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    if (process.waitFor() != 0 || output.isEmpty()) {
      throw new IllegalStateException("could not resolve converter commit: " + output);
    }
    return output;
  }

  private static Path repoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isDirectory(current.resolve("test-fixtures"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("repository root containing test-fixtures was not found");
  }

  private static void recreateDirectory(Path directory) throws IOException {
    if (Files.exists(directory)) {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
          Files.delete(path);
        }
      }
    }
    Files.createDirectories(directory);
  }
}
