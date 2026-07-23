package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import io.github.gabriel0liv.cpmconverter.config.*;
import io.github.gabriel0liv.cpmconverter.geckolib.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmFixtureSnapshotTest {
  @Test
  void fixtureAAndCMatchIntegralSnapshots() throws Exception {
    for (var fixture : List.of("fixture-a-humanoid", "fixture-c-deep-hierarchy")) {
      var p = project(fixture);
      Path expected =
          Path.of("..", "test-fixtures", fixture, "expected", "cpm-static-graph.json").normalize();
      if (Boolean.getBoolean("generateSnapshots")) {
        Files.writeString(
            expected,
            new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(CpmLogicalProjectionSnapshot.write(p)));
      }
      assertTrue(Files.exists(expected), fixture + " snapshot missing");
      var tree = new ObjectMapper();
      assertEquals(
          tree.readTree(Files.readString(expected)),
          CpmLogicalProjectionSnapshot.write(p),
          fixture);
    }
  }

  @Test
  void fixtureBAndDSmokeHaveCompleteIndexes() throws Exception {
    for (var fixture : List.of("fixture-b-neck", "fixture-d-quadruped")) {
      var projection = project(fixture);
      assertEquals(6, projection.project().roots().size());
      assertEquals(
          projection.index().boneTargets().size(),
          projection.index().boneTargets().keySet().size());
      assertFalse(projection.index().boneTargets().isEmpty());
      assertFalse(projection.index().cubeTargets().isEmpty(), fixture);
      assertTrue(
          new CpmLogicalProjectionValidator()
              .validate(projection).all().stream()
                  .noneMatch(
                      d ->
                          d.severity()
                              == io.github.gabriel0liv.cpmconverter.diagnostics.Severity.ERROR));
    }
  }

  @Test
  void projectionIsDeterministicAcrossTwoExecutions() throws Exception {
    for (var fixture :
        List.of(
            "fixture-a-humanoid",
            "fixture-b-neck",
            "fixture-c-deep-hierarchy",
            "fixture-d-quadruped")) {
      var first = project(fixture);
      var second = project(fixture);
      assertEquals(first, second, fixture);
      assertEquals(
          CpmLogicalProjectionSnapshot.write(first),
          CpmLogicalProjectionSnapshot.write(second),
          fixture);
    }
  }

  private CpmStaticProjection project(String fixture) throws Exception {
    Path d = Path.of("..", "test-fixtures", fixture).normalize();
    var g =
        new GeckoGeometryParser()
            .parse(d.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
    assertTrue(g.success(), g.diagnostics().all().toString());
    var m =
        new GeckoStaticModelAssembler()
            .assemble(g.value(), d.resolve("texture.png"), StaticModelAssemblyRequest.defaults());
    assertTrue(m.success(), m.diagnostics().all().toString());
    var clips =
        new GeckoAnimationParser()
            .parse(
                List.of(
                    new AnimationInput(
                        d.resolve("animations.animation.json"),
                        new io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath(
                            "fixtures/" + fixture + "/animations.animation.json"))),
                m.value(),
                AnimationParseRequest.defaults());
    assertTrue(clips.success(), clips.diagnostics().all().toString());
    var animated = new GeckoAnimatedModelAssembler().attach(m.value(), clips.value());
    assertTrue(animated.success(), animated.diagnostics().all().toString());
    var mapping = new MappingLoader().load(d.resolve("mapping.yaml"));
    assertTrue(mapping.success(), mapping.diagnostics().all().toString());
    var compiled = new MappingCompiler().compile(mapping.value(), new ModelIndex(animated.value()));
    assertTrue(compiled.success(), compiled.diagnostics().all().toString());
    var p = new CpmStaticProjector().project(animated.value(), compiled.value());
    assertTrue(p.success(), p.diagnostics().all().toString());
    return p.value();
  }
}
