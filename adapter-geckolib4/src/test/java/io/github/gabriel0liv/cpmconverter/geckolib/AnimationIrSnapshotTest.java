package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimationIrSnapshotTest {
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void fixtureASnapshot() throws Exception {
    assertFixture("fixture-a-humanoid");
  }

  @Test
  void fixtureBSnapshot() throws Exception {
    assertFixture("fixture-b-neck");
  }

  private void assertFixture(String fixture) throws Exception {
    Path dir = Path.of("..", "test-fixtures", fixture).normalize();
    var geometry =
        new GeckoGeometryParser()
            .parse(dir.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
    assertTrue(geometry.success(), String.valueOf(geometry.diagnostics().all()));
    var model =
        new GeckoStaticModelAssembler()
            .assemble(
                geometry.value(),
                dir.resolve("texture.png"),
                StaticModelAssemblyRequest.defaults());
    assertTrue(model.success(), String.valueOf(model.diagnostics().all()));
    var parsed =
        new GeckoAnimationParser()
            .parse(
                List.of(
                    new AnimationInput(
                        dir.resolve("animations.animation.json"),
                        new SourcePath("fixtures/" + fixture + "/animations.animation.json"))),
                model.value(),
                AnimationParseRequest.defaults());
    assertTrue(parsed.success(), String.valueOf(parsed.diagnostics().all()));
    var attached = new GeckoAnimatedModelAssembler().attach(model.value(), parsed.value());
    assertTrue(attached.success(), String.valueOf(attached.diagnostics().all()));
    JsonNode observed = AnimationIrSnapshot.write(attached.value().clips());
    Path expectedPath = dir.resolve("expected/animation-ir.json");
    if (Boolean.getBoolean("writeSnapshots")) {
      Files.writeString(
          expectedPath, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(observed) + "\n");
      return;
    }
    assertEquals(JSON.readTree(Files.readString(expectedPath)), observed, fixture);
  }
}
