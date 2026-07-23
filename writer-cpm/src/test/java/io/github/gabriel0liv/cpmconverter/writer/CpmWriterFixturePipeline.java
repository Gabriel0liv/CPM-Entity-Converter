package io.github.gabriel0liv.cpmconverter.writer;

import io.github.gabriel0liv.cpmconverter.config.*;
import io.github.gabriel0liv.cpmconverter.geckolib.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.projection.*;
import java.nio.file.*;
import java.util.*;

final class CpmWriterFixturePipeline {
  record WriterFixtureResult(
      ModelIR model,
      SemanticRigMap mapping,
      CpmStaticProjection logicalProjection,
      CpmIdentifiedProjectionV1 identifiedProjection,
      CpmProjectArtifact artifact,
      byte[] sourcePng,
      byte[] configJson,
      byte[] persistedPng,
      CpmArtifactInspector.InspectedArtifact inspected) {}

  static WriterFixtureResult run(String fixture) throws Exception {
    Path d = Path.of("..", "test-fixtures", fixture).normalize();
    var geometry = new GeckoGeometryParser().parse(d.resolve("geometry.geo.json"), GeometryParseRequest.defaults());
    if (!geometry.success()) throw new AssertionError(geometry.diagnostics().all().toString());
    var model = new GeckoStaticModelAssembler().assemble(geometry.value(), d.resolve("texture.png"), StaticModelAssemblyRequest.defaults());
    if (!model.success()) throw new AssertionError(model.diagnostics().all().toString());
    var clips = new GeckoAnimationParser().parse(List.of(new AnimationInput(d.resolve("animations.animation.json"), new io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath("fixtures/" + fixture + "/animations.animation.json"))), model.value(), AnimationParseRequest.defaults());
    if (!clips.success()) throw new AssertionError(clips.diagnostics().all().toString());
    var animated = new GeckoAnimatedModelAssembler().attach(model.value(), clips.value());
    if (!animated.success()) throw new AssertionError(animated.diagnostics().all().toString());
    var mapping = new MappingLoader().load(d.resolve("mapping.yaml"));
    if (!mapping.success()) throw new AssertionError(mapping.diagnostics().all().toString());
    var compiled = new MappingCompiler().compile(mapping.value(), new ModelIndex(animated.value()));
    if (!compiled.success()) throw new AssertionError(compiled.diagnostics().all().toString());
    var projected = new CpmStaticProjector().project(animated.value(), compiled.value());
    if (!projected.success()) throw new AssertionError(projected.diagnostics().all().toString());
    var identified = new CpmStoreIdAssigner().assign(projected.value());
    if (!identified.success()) throw new AssertionError(identified.diagnostics().all().toString());
    var png = Files.readAllBytes(d.resolve("texture.png"));
    var artifact = new CpmProjectWriter().write(new CpmProjectWriteRequest(identified.value(), png));
    if (!artifact.success()) throw new AssertionError(artifact.diagnostics().all().toString());
    var inspected = CpmArtifactInspector.inspect(artifact.value().bytes());
    var config = inspected.entries().stream().filter(e -> e.name().equals("config.json")).findFirst().orElseThrow();
    var persisted = inspected.entries().stream().filter(e -> e.name().equals("skin.png")).findFirst().orElseThrow();
    return new WriterFixtureResult(animated.value(), compiled.value(), projected.value(), identified.value(), artifact.value(), png.clone(), config.contents(), persisted.contents(), inspected);
  }
}
