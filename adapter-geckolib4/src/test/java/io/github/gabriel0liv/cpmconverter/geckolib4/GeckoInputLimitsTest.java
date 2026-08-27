package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class GeckoInputLimitsTest {
  private static final byte[] PNG_2X2 =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR4nGP8z8Dwn4GBgYGJAQoAHxcCAk+Uzr4AAAAASUVORK5CYII=");

  @Test
  void rejectsGeometryBeforeReadingJsonWhenFileExceedsByteLimit() throws Exception {
    Path input =
        geometry(
            """
            {"format_version":"1.12.0","minecraft:geometry":[{
              "description":{"identifier":"demo:bytes","texture_width":16,"texture_height":16},
              "bones":[{"name":"body","pivot":[0,0,0]}]
            }]}
            """);
    var limits = new GeckoInputLimits(32, 64, 10, 10, 10);

    var result = new GeckoGeometryParser(limits).parse(input);

    assertLimitFailure(result);
  }

  @Test
  void rejectsJsonThatExceedsConfiguredNestingDepth() throws Exception {
    String nested = "0";
    for (int i = 0; i < 40; i++) nested = "{\"x\":" + nested + "}";
    Path input =
        geometry(
            """
            {"format_version":"1.12.0","extra":%s,"minecraft:geometry":[{
              "description":{"identifier":"demo:depth","texture_width":16,"texture_height":16},
              "bones":[{"name":"body","pivot":[0,0,0]}]
            }]}
            """
                .formatted(nested));
    var limits = new GeckoInputLimits(1_000_000, 16, 10, 10, 10);

    var result = new GeckoGeometryParser(limits).parse(input);

    assertLimitFailure(result);
  }

  @Test
  void rejectsBoneAndCubeCountsAtParserBoundary() throws Exception {
    Path tooManyBones =
        geometry(
            """
            {"format_version":"1.12.0","minecraft:geometry":[{
              "description":{"identifier":"demo:bones","texture_width":16,"texture_height":16},
              "bones":[
                {"name":"a","pivot":[0,0,0]},
                {"name":"b","pivot":[0,0,0]}
              ]
            }]}
            """);
    var boneLimits = new GeckoInputLimits(1_000_000, 64, 1, 100, 100);
    assertLimitFailure(new GeckoGeometryParser(boneLimits).parse(tooManyBones));

    Path tooManyCubes =
        geometry(
            """
            {"format_version":"1.12.0","minecraft:geometry":[{
              "description":{"identifier":"demo:cubes","texture_width":16,"texture_height":16},
              "bones":[{"name":"body","pivot":[0,0,0],"cubes":[
                {"origin":[0,0,0],"size":[1,1,1],"uv":[0,0]},
                {"origin":[1,0,0],"size":[1,1,1],"uv":[0,0]}
              ]}]
            }]}
            """);
    var cubeLimits = new GeckoInputLimits(1_000_000, 64, 100, 1, 100);
    assertLimitFailure(new GeckoGeometryParser(cubeLimits).parse(tooManyCubes));
  }

  @Test
  void rejectsAnimationWhenTotalKeyframesExceedLimit() throws Exception {
    Path input =
        animation(
            """
            {"animations":{"a":{"animation_length":1.0,"bones":{"body":{"rotation":{
              "0.0":[0,0,0],
              "0.5":[1,2,3],
              "1.0":[2,4,6]
            }}}}}}
            """);
    var limits = new GeckoInputLimits(1_000_000, 64, 100, 100, 2);

    var result = new GeckoAnimationParser(limits).parse(input, geometryModel());

    assertLimitFailure(result);
  }

  @Test
  void rejectsExplicitAndDerivedAnimationDurationBeyondConfiguredCeiling() throws Exception {
    var limits = new GeckoInputLimits(1_000_000, 64, 100, 100, 100, 2.0, 1_000_000, 1_000_000);

    Path explicit =
        animation(
            """
            {"animations":{"a":{"animation_length":3.0,"bones":{"body":{"rotation":[0,0,0]}}}}}
            """);
    assertLimitFailure(new GeckoAnimationParser(limits).parse(explicit, geometryModel()));

    Path derived =
        animation(
            """
            {"animations":{"a":{"bones":{"body":{"rotation":{
              "0.0":[0,0,0],
              "3.0":[1,2,3]
            }}}}}}
            """);
    assertLimitFailure(new GeckoAnimationParser(limits).parse(derived, geometryModel()));
  }

  @Test
  void geometryTextureLoadPropagatesConfiguredPngLimits() throws Exception {
    Path input =
        geometry(
            """
            {"format_version":"1.12.0","minecraft:geometry":[{
              "description":{"identifier":"demo:texture-limits","texture_width":2,"texture_height":2},
              "bones":[{"name":"body","pivot":[0,0,0]}]
            }]}
            """);
    Path png = Files.createTempFile("cpm-converter-limit-", ".png");
    Files.write(png, PNG_2X2);
    var limits = new GeckoInputLimits(1_000_000, 64, 100, 100, 100, 60.0, 16, 1_000_000);

    var result = new GeckoGeometryParser(limits).parse(input, null, png);

    assertLimitFailure(result);
  }

  @Test
  void defaultLimitsKeepNormalFixtureWithinSupportedEnvelope() {
    var result = new GeckoGeometryParser().parse(fixtureGeometry());

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
  }

  private static ModelIR geometryModel() {
    var result = new GeckoGeometryParser().parse(fixtureGeometry());
    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    return result.value();
  }

  private static Path fixtureGeometry() {
    return Path.of("..", "test-fixtures", "fixture-a-humanoid", "geometry.geo.json").normalize();
  }

  private static Path geometry(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-limit-", ".geo.json");
    Files.writeString(path, json);
    return path;
  }

  private static Path animation(String json) throws Exception {
    Path path = Files.createTempFile("cpm-converter-limit-", ".animation.json");
    Files.writeString(path, json);
    return path;
  }

  private static void assertLimitFailure(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result) {
    assertFalse(result.success());
    assertEquals(1, result.diagnostics().errors().size(), result.diagnostics().all().toString());
    assertEquals(
        DiagnosticCodes.INPUT_LIMIT_EXCEEDED, result.diagnostics().errors().get(0).code().value());
  }
}
