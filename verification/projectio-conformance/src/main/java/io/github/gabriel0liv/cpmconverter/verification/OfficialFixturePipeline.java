package io.github.gabriel0liv.cpmconverter.verification;

import io.github.gabriel0liv.cpmconverter.config.*;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.geckolib.*;
import io.github.gabriel0liv.cpmconverter.ir.ModelIndex;
import io.github.gabriel0liv.cpmconverter.projection.*;
import io.github.gabriel0liv.cpmconverter.writer.*;
import java.nio.file.*;
import java.util.*;

/** VERIFICATION_ONLY: invokes the official T302 pipeline without ZIP reconstruction. */
final class OfficialFixturePipeline {
  private OfficialFixturePipeline() {}

  static byte[] write(String fixture) throws Exception {
    Path directory = fixtureDirectory(fixture);
    var geometry =
        new GeckoGeometryParser()
            .parse(directory.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
    require(geometry, "geometry");
    var model =
        new GeckoStaticModelAssembler()
            .assemble(
                geometry.value(),
                directory.resolve("texture.png"),
                StaticModelAssemblyRequest.defaults());
    require(model, "model");
    var clips =
        new GeckoAnimationParser()
            .parse(
                List.of(
                    new AnimationInput(
                        directory.resolve("animations.animation.json"),
                        new io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath(
                            "fixtures/" + fixture + "/animations.animation.json"))),
                model.value(),
                AnimationParseRequest.defaults());
    require(clips, "animations");
    var animated = new GeckoAnimatedModelAssembler().attach(model.value(), clips.value());
    require(animated, "animated model");
    var mapping = new MappingLoader().load(directory.resolve("mapping.yaml"));
    require(mapping, "mapping");
    var compiled = new MappingCompiler().compile(mapping.value(), new ModelIndex(animated.value()));
    require(compiled, "compiled mapping");
    var projected = new CpmStaticProjector().project(animated.value(), compiled.value());
    require(projected, "projection");
    var identified = new CpmStoreIdAssigner().assign(projected.value());
    require(identified, "store ids");
    var artifact =
        new CpmProjectWriter()
            .write(
                new CpmProjectWriteRequest(
                    identified.value(), Files.readAllBytes(directory.resolve("texture.png"))));
    require(artifact, "writer");
    return artifact.value().bytes();
  }

  private static Path fixtureDirectory(String fixture) {
    Path direct = Path.of("test-fixtures", fixture);
    return Files.isDirectory(direct) ? direct : Path.of("../../test-fixtures", fixture).normalize();
  }

  private static void require(Result<?> result, String phase) {
    if (!result.success()) throw new AssertionError(phase + ": " + result.diagnostics().all());
  }
}
