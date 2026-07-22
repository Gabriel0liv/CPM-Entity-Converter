package io.github.gabriel0liv.cpmconverter.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.config.MappingCompiler;
import io.github.gabriel0liv.cpmconverter.config.MappingDocumentV1;
import io.github.gabriel0liv.cpmconverter.config.MappingLoader;
import io.github.gabriel0liv.cpmconverter.config.SemanticRigMap;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import io.github.gabriel0liv.cpmconverter.ir.CubeIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.ir.FeatureOccurrence;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIndex;
import io.github.gabriel0liv.cpmconverter.ir.PlaybackMode;
import io.github.gabriel0liv.cpmconverter.ir.SourceDescriptor;
import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** NON_PRODUCTION / FIXTURE_ONLY: verifies the mapping contract with a minimal IR. */
class FixtureSemanticContractTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final List<String> FIXTURES =
      List.of(
          "fixture-a-humanoid",
          "fixture-b-neck",
          "fixture-c-deep-hierarchy",
          "fixture-d-quadruped");

  @Test
  void mappingCompilerContractsMatchExpected() throws Exception {
    Path root = fixtureRoot();
    MappingLoader loader = new MappingLoader();
    for (String fixture : FIXTURES) {
      Path directory = root.resolve(fixture);
      var mapping = loader.load(directory.resolve("mapping.yaml"));
      assertTrue(mapping.success(), fixture + ": " + mapping.diagnostics());
      ModelIR model = fixtureModel(directory, fixture);
      var compiled =
          new MappingCompiler()
              .compile(qualifyClips(mapping.value(), fixture), new ModelIndex(model));
      assertTrue(compiled.success(), fixture + ": " + compiled.diagnostics().all());
      JsonNode expected =
          JSON.readTree(directory.resolve("expected/mapping-compiled.json").toFile());
      assertEquals(expected, canonical(compiled.value()), fixture);
      assertEquals(
          JSON.readTree(directory.resolve("expected/inventory.json").toFile()),
          inventoryObservation(directory, fixture),
          fixture + " inventory");
      // Diagnostics for unsupported quadruped projection and full geometry/animation
      // invariants belong to the production parser/projection tasks (T200-T204),
      // not to this Phase 1 mapping smoke contract.  The manifest performs the
      // authorial inventory, provenance and structural checks independently.
    }
  }

  private static JsonNode inventoryObservation(Path directory, String fixture) throws Exception {
    JsonNode geometry = JSON.readTree(directory.resolve("geometry.geo.json").toFile());
    JsonNode geometryRoot = geometry.path("minecraft:geometry").get(0);
    var result = JSON.createObjectNode();
    result.put(
        "animationFormat",
        JSON.readTree(directory.resolve("animations.animation.json").toFile())
            .path("format_version")
            .asText());
    var names = JSON.createArrayNode();
    geometryRoot.path("bones").forEach(bone -> names.add(bone.path("name").asText()));
    result.set("bones", names);
    result.put("fixture", fixture);
    result.put("geometryFormat", geometry.path("format_version").asText());
    result.put("hasCubes", true);
    return result;
  }

  private static MappingDocumentV1 qualifyClips(MappingDocumentV1 value, String fixture) {
    Map<String, String> clips = new LinkedHashMap<>();
    value.clips().forEach((key, clip) -> clips.put(key, fixture + ":" + clip));
    Map<String, MappingDocumentV1.StateMapping> states = new LinkedHashMap<>();
    value
        .stateMappings()
        .forEach(
            (key, state) ->
                states.put(
                    key,
                    new MappingDocumentV1.StateMapping(
                        fixture + ":" + state.clip(),
                        state.mode(),
                        state.optional(),
                        state.requestedFps())));
    return new MappingDocumentV1(
        value.schemaVersion(),
        value.modelScale(),
        value.verticalOffset(),
        value.skin(),
        value.rootStrategy(),
        value.rootRoles(),
        value.bones(),
        clips,
        value.look(),
        states,
        value.sampling(),
        value.ignore(),
        value.diagnosticPolicy());
  }

  private static ModelIR fixtureModel(Path directory, String fixture) throws Exception {
    JsonNode geometry = JSON.readTree(directory.resolve("geometry.geo.json").toFile());
    JsonNode description = geometry.path("minecraft:geometry").get(0).path("description");
    List<BoneIR> bones = new ArrayList<>();
    for (JsonNode sourceBone : geometry.path("minecraft:geometry").get(0).path("bones")) {
      String name = sourceBone.path("name").asText();
      BoneId id = new BoneId(fixture + ":" + name);
      List<CubeIR> cubes = new ArrayList<>();
      int index = 0;
      for (JsonNode cube : sourceBone.path("cubes")) {
        JsonNode origin = cube.path("origin");
        JsonNode size = cube.path("size");
        JsonNode uv = cube.path("uv");
        cubes.add(
            new CubeIR(
                new CubeId(fixture + ":" + name + ":cube" + index++),
                id,
                vector(origin, Vec3d.ZERO),
                vector(size, new Vec3d(1, 1, 1)),
                vector(cube.path("pivot"), Vec3d.ZERO),
                Quatd.IDENTITY,
                cube.path("inflate").asDouble(0),
                cube.path("mirror").asBoolean(false),
                new BoxUvIR(
                    uv.isArray() ? uv.path(0).asInt() : 0, uv.isArray() ? uv.path(1).asInt() : 0),
                fixture + "/geometry.geo.json"));
      }
      String parent = sourceBone.has("parent") ? sourceBone.path("parent").asText() : null;
      List<BoneId> children = new ArrayList<>();
      for (JsonNode candidate : geometry.path("minecraft:geometry").get(0).path("bones")) {
        if (candidate.has("parent") && name.equals(candidate.path("parent").asText())) {
          children.add(new BoneId(fixture + ":" + candidate.path("name").asText()));
        }
      }
      bones.add(
          new BoneIR(
              id,
              name,
              parent == null ? null : new BoneId(fixture + ":" + parent),
              children,
              Transform.identity(),
              cubes,
              SourceLocation.of(new SourcePath(fixture + "/geometry.geo.json"))));
    }
    JsonNode animations = JSON.readTree(directory.resolve("animations.animation.json").toFile());
    List<AnimationClipIR> clips = new ArrayList<>();
    animations
        .path("animations")
        .fieldNames()
        .forEachRemaining(
            name ->
                clips.add(
                    new AnimationClipIR(
                        new ClipId(fixture + ":" + name),
                        1,
                        PlaybackMode.LOOP,
                        null,
                        List.of(),
                        List.of(),
                        SourceLocation.of(
                            new SourcePath(
                                fixture + "/animations.animation.json#/animations/" + name)))));
    List<BoneId> roots =
        bones.stream().filter(bone -> bone.parent() == null).map(BoneIR::id).toList();
    return new ModelIR(
        new SourceDescriptor(fixture + "/geometry.geo.json", "geometry-1.12.0"),
        new io.github.gabriel0liv.cpmconverter.ir.GeometryId(fixture + ":geometry"),
        bones,
        roots,
        clips,
        List.of(
            new TextureIR(
                fixture + "/texture.png",
                description.path("texture_width").asInt(),
                description.path("texture_height").asInt())),
        List.<FeatureOccurrence>of());
  }

  private static Vec3d vector(JsonNode node, Vec3d fallback) {
    return node != null && node.isArray() && node.size() == 3
        ? new Vec3d(node.get(0).asDouble(), node.get(1).asDouble(), node.get(2).asDouble())
        : fallback;
  }

  private static JsonNode canonical(SemanticRigMap map) {
    var out = JSON.createObjectNode();
    var bones = JSON.createObjectNode();
    map.bones().forEach((key, value) -> bones.put(key, value.value()));
    out.set("boneIds", bones);
    var clips = JSON.createObjectNode();
    map.clips().forEach((key, value) -> clips.put(key, value.value()));
    out.set("clipIds", clips);
    var roots = JSON.createObjectNode();
    map.rootRoles().roles().forEach((key, value) -> roots.put(key, value.value()));
    out.set("rootRoles", roots);
    if (map.look() != null) {
      var look = JSON.createObjectNode();
      map.look().head().ifPresent(value -> look.put("head", value.value()));
      map.look().neck().ifPresent(value -> look.put("neck", value.value()));
      look.put("composition", map.look().composition());
      look.put("neckInfluence", map.look().neckInfluence());
      look.put("headInfluence", map.look().headInfluence());
      look.put("allowOverrotation", map.look().allowOverrotation());
      out.set("look", look);
    }
    var states = JSON.createObjectNode();
    map.stateMappings()
        .forEach(
            (key, value) -> {
              var state = JSON.createObjectNode();
              state.put("clipId", value.clip().value());
              state.put("mode", value.mode());
              state.put("optional", value.optional());
              if (value.requestedFps() != null) state.put("requestedFps", value.requestedFps());
              states.set(key, state);
            });
    out.set("stateMappings", states);
    if (map.sampling() != null)
      out.putObject("sampling").put("requestedFps", map.sampling().requestedFps());
    var ignores = JSON.createArrayNode();
    map.ignore()
        .forEach(
            rule ->
                ignores.addObject().put("feature", rule.feature()).put("reason", rule.reason()));
    out.set("ignoreRules", ignores);
    if (map.diagnosticPolicy() != null) {
      out.putObject("diagnosticPolicy")
          .put("warningsAsErrors", map.diagnosticPolicy().warningsAsErrors())
          .put("ignoreUnsupported", map.diagnosticPolicy().ignoreUnsupported());
    }
    return out;
  }

  private static Path fixtureRoot() throws Exception {
    Path current = Path.of(".").toAbsolutePath().normalize();
    if (Files.isDirectory(current.resolve("fixture-a-humanoid"))) return current;
    if (Files.isDirectory(current.resolve("test-fixtures/fixture-a-humanoid")))
      return current.resolve("test-fixtures");
    throw new IllegalStateException("cannot locate fixture root from " + current);
  }
}
