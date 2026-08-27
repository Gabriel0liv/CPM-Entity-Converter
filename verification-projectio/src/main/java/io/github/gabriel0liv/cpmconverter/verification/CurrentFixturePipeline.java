package io.github.gabriel0liv.cpmconverter.verification;

import io.github.gabriel0liv.cpmconverter.config.MappingCompiler;
import io.github.gabriel0liv.cpmconverter.config.MappingLoader;
import io.github.gabriel0liv.cpmconverter.cpm.CpmProjectWriterV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmProjectionSettings;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStaticProjectV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStaticProjector;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStoreIdAllocator;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStoreIdPlan;
import io.github.gabriel0liv.cpmconverter.cpm.validation.CpmProjectValidator;
import io.github.gabriel0liv.cpmconverter.cpm.validation.CpmValidationProfile;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.geckolib4.GeckoAnimationParser;
import io.github.gabriel0liv.cpmconverter.geckolib4.GeckoGeometryParser;
import io.github.gabriel0liv.cpmconverter.geckolib4.GeckoTextureLoader;
import io.github.gabriel0liv.cpmconverter.ir.AnimationClipIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIndex;
import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** Generates T304 artifacts through the current production static CPM pipeline. */
public final class CurrentFixturePipeline {
  public static final List<String> FIXTURES =
      List.of(
          "fixture-a-humanoid",
          "fixture-b-neck",
          "fixture-c-deep-hierarchy",
          "fixture-d-quadruped");

  public CurrentFixtureArtifact generate(String fixture) throws Exception {
    if (!FIXTURES.contains(fixture)) throw new IllegalArgumentException("unknown fixture " + fixture);

    Path directory = repoRoot().resolve("test-fixtures").resolve(fixture);
    ModelIR geometry =
        require(
            new GeckoGeometryParser().parse(directory.resolve("geometry.geo.json")), "geometry");
    List<AnimationClipIR> clips =
        require(
            new GeckoAnimationParser()
                .parse(directory.resolve("animations.animation.json"), geometry),
            "animations");
    TextureIR texture =
        require(
            new GeckoTextureLoader().load(directory.resolve("texture.png"), 32, 32), "texture");

    ModelIR completeModel =
        new ModelIR(
            geometry.source(),
            geometry.geometryId(),
            geometry.bones(),
            geometry.roots(),
            clips,
            List.of(texture),
            geometry.unsupportedFeatures());

    var mapping = require(new MappingLoader().load(directory.resolve("mapping.yaml")), "mapping");
    var compiled =
        require(
            new MappingCompiler().compile(mapping, new ModelIndex(completeModel)),
            "compiled mapping");

    double modelScale = compiled.modelScale() == null ? 1.0 : compiled.modelScale();
    double verticalOffset = compiled.verticalOffset() == null ? 0.0 : compiled.verticalOffset();
    CpmProjectionSettings settings =
        new CpmProjectionSettings(modelScale, verticalOffset, true, true);

    CpmStaticProjectV1 projected =
        require(new CpmStaticProjector().project(completeModel, settings), "projection");
    CpmStoreIdPlan storeIds =
        require(new CpmStoreIdAllocator().allocate(projected), "store ids");
    byte[] bytes = require(new CpmProjectWriterV1().write(projected, storeIds), "writer");

    require(
        new CpmProjectValidator().validate(bytes, CpmValidationProfile.GENERATED_V1),
        "generated validator");
    return new CurrentFixtureArtifact(fixture, projected, storeIds, bytes, sha256(bytes));
  }

  private static <T> T require(Result<T> result, String phase) {
    if (!result.success()) {
      throw new AssertionError(phase + ": " + result.diagnostics().all());
    }
    return result.value();
  }

  private static Path repoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isDirectory(current.resolve("test-fixtures"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("repository root containing test-fixtures was not found");
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
