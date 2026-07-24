package io.github.gabriel0liv.cpmconverter.verification;

import com.google.gson.*;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.elements.ModelElement;
import com.tom.cpm.shared.editor.project.ProjectFile;
import com.tom.cpm.shared.editor.project.ProjectIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import spike.ProjectIoOracle;

/** VERIFICATION_ONLY: headless ProjectIO conformance for T304. */
public final class T304ProjectIoConformance {
  private static final String CPM_COMMIT = "9272f4f9c36a2bbd6986e6da65bf7091369cb12b";
  private static final Map<String, String> GOLDENS =
      Map.of(
          "fixture-a-humanoid", "31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97",
          "fixture-b-neck", "4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9",
          "fixture-c-deep-hierarchy",
              "177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3",
          "fixture-d-quadruped",
              "82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695");
  private static final List<String> FIXTURES =
      List.of(
          "fixture-a-humanoid",
          "fixture-b-neck",
          "fixture-c-deep-hierarchy",
          "fixture-d-quadruped");

  private T304ProjectIoConformance() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1)
      throw new IllegalArgumentException("expected generate, conformance or bundle");
    switch (args[0]) {
      case "generate" -> generate();
      case "conformance" -> conformance();
      case "bundle" -> bundle();
      default -> throw new IllegalArgumentException("unknown mode: " + args[0]);
    }
  }

  private static void generate() throws Exception {
    Path output = repoRoot().resolve("build/t304/artifacts");
    Files.createDirectories(output);
    JsonArray manifest = new JsonArray();
    for (String fixture : FIXTURES) {
      byte[] first = OfficialFixturePipeline.write(fixture);
      byte[] second = OfficialFixturePipeline.write(fixture);
      if (!Arrays.equals(first, second))
        throw new IllegalStateException(fixture + " is not deterministic");
      String hash = sha(first);
      if (!GOLDENS.get(fixture).equals(hash))
        throw new IllegalStateException(
            fixture + " hash mismatch expected=" + GOLDENS.get(fixture) + " observed=" + hash);
      Path file = output.resolve(fixture + ".cpmproject");
      Files.write(file, first);
      JsonObject entry = new JsonObject();
      entry.addProperty("fixture", fixture);
      entry.addProperty("sha256", hash);
      entry.addProperty("bytes", first.length);
      manifest.add(entry);
    }
    writeJson(repoRoot().resolve("build/t304/artifacts/manifest.json"), manifest);
  }

  private static void conformance() throws Exception {
    generate();
    ProjectIoOracle.initializeHeadlessAccess();
    JsonArray reports = new JsonArray();
    for (String fixture : FIXTURES) {
      Path file = repoRoot().resolve("build/t304/artifacts/" + fixture + ".cpmproject");
      reports.add(loadReport(fixture, file));
    }
    Path s003 = repoRoot().resolve("spikes/minimal-cpmproject/artifacts");
    for (String name : List.of("M0", "M1", "M2", "M3", "M4", "M5")) {
      reports.add(loadReport(name, s003.resolve(name + ".cpmproject")));
    }
    JsonObject root = new JsonObject();
    root.addProperty("cpmCommit", CPM_COMMIT);
    root.addProperty("cpmVersion", "0.6.27");
    root.add("projects", reports);
    writeJson(repoRoot().resolve("build/t304/projectio/projectio-report.json"), root);
    System.out.println(new GsonBuilder().create().toJson(root));
  }

  private static JsonObject loadReport(String name, Path file) throws Exception {
    JsonObject result = new JsonObject();
    result.addProperty("name", name);
    result.addProperty("sha256", sha(Files.readAllBytes(file)));
    try {
      Editor editor = ProjectIoOracle.loadEditor(file.toFile());
      result.addProperty("success", true);
      result.addProperty("rootCount", editor.elements.size());
      result.addProperty("animationCount", editor.animations.size());
      result.addProperty("containsStoreId1000", containsStoreId(editor.elements, 1000L));
      result.addProperty("animationReferenceCount", animationReferenceCount(editor));
      try {
        Path temporary = Files.createTempFile("t304-roundtrip-", ".cpmproject");
        ProjectFile saved = new ProjectFile();
        ProjectIO.saveProject(editor, saved);
        saved.save(temporary.toFile()).join();
        Editor reopened = ProjectIoOracle.loadEditor(temporary.toFile());
        result.addProperty(
            "roundTrip", reopened.elements.size() == editor.elements.size() ? "PASS" : "FAIL");
        Files.deleteIfExists(temporary);
      } catch (Throwable roundTripFailure) {
        result.addProperty("roundTrip", "NOT_AVAILABLE");
        result.addProperty("roundTripReason", normalize(roundTripFailure.getMessage()));
      }
      JsonArray elements = new JsonArray();
      for (ModelElement element : editor.elements) appendElement(elements, element, "");
      result.add("elements", elements);
    } catch (Throwable error) {
      Throwable current = error;
      while (current.getCause() != null
          && (current instanceof CompletionException || current instanceof ExecutionException))
        current = current.getCause();
      result.addProperty("success", false);
      result.addProperty("errorType", current.getClass().getName());
      result.addProperty("message", normalize(current.getMessage()));
    }
    return result;
  }

  private static void appendElement(JsonArray output, ModelElement element, String parentPath) {
    String displayName = stableName(element);
    String path = parentPath.isEmpty() ? displayName : parentPath + "/" + displayName;
    JsonObject value = new JsonObject();
    value.addProperty("path", path);
    value.addProperty("name", element.name);
    value.addProperty("type", String.valueOf(element.type));
    value.addProperty("storeID", element.storeID);
    value.addProperty("parentPath", parentPath);
    value.addProperty("texture", element.texture);
    value.addProperty("textureSize", element.textureSize);
    value.addProperty("u", element.u);
    value.addProperty("v", element.v);
    value.addProperty("mirror", element.mirror);
    value.addProperty("mcScale", element.mcScale);
    value.add("position", vector(element.pos));
    value.add("rotation", vector(element.rotation));
    value.add("scale", vector(element.scale));
    value.add("meshScale", vector(element.meshScale));
    output.add(value);
    for (ModelElement child : element.children) appendElement(output, child, path);
  }

  private static String stableName(ModelElement element) {
    if (element.typeData != null) {
      try {
        Object value = element.typeData.getClass().getMethod("getName").invoke(element.typeData);
        if (value != null) return String.valueOf(value);
      } catch (ReflectiveOperationException ignored) {
        // The type name is a stable fallback when the fixed upstream changes its accessor.
      }
    }
    return element.name == null ? "" : element.name;
  }

  private static boolean containsStoreId(List<ModelElement> elements, long expected) {
    for (ModelElement element : elements) {
      if (element.storeID == expected) return true;
      if (containsStoreId(element.children, expected)) return true;
    }
    return false;
  }

  private static int animationReferenceCount(Editor editor) {
    int total = 0;
    for (Object animation : editor.animations) {
      try {
        var frames = animation.getClass().getDeclaredField("frames");
        frames.setAccessible(true);
        for (Object frame : (List<?>) frames.get(animation)) {
          var components = frame.getClass().getDeclaredField("components");
          components.setAccessible(true);
          total += ((Map<?, ?>) components.get(frame)).size();
        }
      } catch (ReflectiveOperationException error) {
        return -1;
      }
    }
    return total;
  }

  private static JsonObject vector(Object value) {
    JsonObject result = new JsonObject();
    if (value == null) return result;
    try {
      result.addProperty("x", value.getClass().getField("x").getFloat(value));
      result.addProperty("y", value.getClass().getField("y").getFloat(value));
      result.addProperty("z", value.getClass().getField("z").getFloat(value));
    } catch (ReflectiveOperationException error) {
      result.addProperty("error", error.getClass().getSimpleName());
    }
    return result;
  }

  private static void bundle() throws IOException {
    Path root = repoRoot().resolve("build/t304/manual-evidence");
    Path artifacts = root.resolve("artifacts");
    Files.createDirectories(artifacts);
    Path generated = repoRoot().resolve("build/t304/artifacts");
    for (String fixture : FIXTURES)
      Files.copy(
          generated.resolve(fixture + ".cpmproject"),
          artifacts.resolve(fixture + ".cpmproject"),
          StandardCopyOption.REPLACE_EXISTING);
    JsonObject manifest = new JsonObject();
    manifest.addProperty("status", "NOT RUN");
    manifest.addProperty("cpmCommit", CPM_COMMIT);
    manifest.addProperty("cpmVersion", "0.6.27");
    manifest.addProperty("java", System.getProperty("java.version"));
    manifest.addProperty("converterCommit", currentCommit());
    manifest.addProperty("generatedDate", LocalDate.now().toString());
    manifest.add(
        "artifacts", new JsonParser().parse(Files.readString(generated.resolve("manifest.json"))));
    writeJson(root.resolve("manifest.json"), manifest);
    copyIfPresent(
        repoRoot().resolve("build/t304/projectio/projectio-report.json"),
        root.resolve("projectio-report.json"));
    Files.writeString(
        root.resolve("expected-static-snapshot.json"),
        "{\n  \"contract\": \"T304 static ProjectIO conformance\",\n  \"visualValidation\": \"NOT RUN\",\n  \"fixtures\": {\n    \"fixture-a-humanoid\": {\"rootCount\": 6, \"containsPersistedIds\": true},\n    \"fixture-b-neck\": {\"rootCount\": 6, \"containsPersistedIds\": true},\n    \"fixture-c-deep-hierarchy\": {\"rootCount\": 6, \"containsPersistedIds\": true},\n    \"fixture-d-quadruped\": {\"rootCount\": 6, \"containsPersistedIds\": true}\n  }\n}\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("README.md"),
        "# T304 manual evidence\n\nAutomated ProjectIO conformance is recorded in projectio-report.json. Visual validation is NOT RUN.\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("checklist.md"),
        "# T304 human checklist\n\nTester:  \nDate:  \nOS:  \n\nA/B/C/D open, texture, UV, hierarchy, Save As/reopen: NOT RUN\n",
        StandardCharsets.UTF_8);
    for (String fixture : FIXTURES) Files.createDirectories(root.resolve("screenshots/" + fixture));
    Files.createDirectories(root.resolve("round-trip"));
  }

  private static void copyIfPresent(Path source, Path target) throws IOException {
    if (Files.isRegularFile(source))
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  private static String normalize(String message) {
    return message == null ? "" : message.replaceAll("\\s+", " ").trim();
  }

  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static String currentCommit() {
    try {
      Process process =
          new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
      return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    } catch (Exception error) {
      return "UNKNOWN";
    }
  }

  private static Path repoRoot() {
    return Files.isDirectory(Path.of("test-fixtures"))
        ? Path.of(".").toAbsolutePath().normalize()
        : Path.of("../.. ".trim()).toAbsolutePath().normalize();
  }

  private static void writeJson(Path path, JsonElement json) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(
        path,
        new GsonBuilder().disableHtmlEscaping().create().toJson(json) + "\n",
        StandardCharsets.UTF_8);
  }
}
