package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeckoGeometryParserTest {
  private final GeckoGeometryParser parser = new GeckoGeometryParser();

  @Test
  void parsesFixtureAInSourceOrderAndBuildsLocalHierarchy() {
    var result = parser.parse(fixture("fixture-a-humanoid"), null);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    ModelIR model = result.value();
    assertEquals("cpm:a", model.geometryId().value());
    assertEquals(
        List.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"),
        model.bones().stream().map(bone -> bone.name()).toList());
    assertEquals(List.of("body"), model.roots().stream().map(id -> boneName(model, id.value())).toList());

    var body = model.bones().get(0);
    var head = model.bones().get(1);
    assertNull(body.parent());
    assertEquals(body.id(), head.parent());
    assertEquals(
        List.of("head", "left_arm", "right_arm", "left_leg", "right_leg"),
        body.children().stream().map(id -> boneName(model, id.value())).toList());
    assertEquals(Vec3d.ZERO, body.bind().translation());
    assertEquals(new Vec3d(0, -8, 0), head.bind().translation());

    assertEquals(1, head.cubes().size());
    var cube = head.cubes().get(0);
    assertEquals(new Vec3d(-3, -2, 0), cube.origin());
    assertEquals(new Vec3d(2, 2, 2), cube.size());
    assertEquals(new Vec3d(0, -8, 0), cube.pivot());
    assertInstanceOf(BoxUvIR.class, cube.uv());
    assertEquals(new BoxUvIR(1, 1), cube.uv());
  }

  @Test
  void parsesFixtureCDeepHierarchyAndOwnCubePivotRotation() {
    var result = parser.parse(fixture("fixture-c-deep-hierarchy"), null);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    ModelIR model = result.value();
    var head = model.bones().stream().filter(bone -> bone.name().equals("head")).findFirst().orElseThrow();
    var jaw = model.bones().stream().filter(bone -> bone.name().equals("jaw")).findFirst().orElseThrow();
    var accessory =
        model.bones().stream().filter(bone -> bone.name().equals("accessory")).findFirst().orElseThrow();

    assertEquals(head.id(), jaw.parent());
    assertEquals(head.id(), accessory.parent());
    assertEquals(1, accessory.cubes().size());
    var cube = accessory.cubes().get(0);
    assertEquals(new Vec3d(-1.5, -2.5, 0.5), cube.pivot());
    assertEquals(
        new Vec3d(-12, 0, 27),
        cube.rotation().toEulerZYX().toDegrees(new Vec3d(-12, 0, 27)));
    assertInstanceOf(PerFaceUvIR.class, cube.uv());
  }

  @Test
  void requiresGeometrySelectorWhenInputContainsMultipleModels() throws Exception {
    Path input =
        tempGeometry(
            "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":["
                + geometry("test:first", "first")
                + ","
                + geometry("test:second", "second")
                + "]}");

    var ambiguous = parser.parse(input, null);
    var selected = parser.parse(input, "test:second");

    assertFalse(ambiguous.success());
    assertDiagnostic(ambiguous, DiagnosticCodes.GEO_MULTIPLE_MODELS);
    assertTrue(selected.success(), () -> selected.diagnostics().all().toString());
    assertEquals("test:second", selected.value().geometryId().value());
    assertEquals("second", selected.value().bones().get(0).name());
  }

  @Test
  void rejectsUnsupportedGeometryVersion() throws Exception {
    Path input =
        tempGeometry(
            "{\"format_version\":\"1.16.0\",\"minecraft:geometry\":["
                + geometry("test:model", "root")
                + "]}");

    var result = parser.parse(input, null);

    assertFalse(result.success());
    assertDiagnostic(result, DiagnosticCodes.INPUT_UNSUPPORTED_VERSION);
  }

  @Test
  void rejectsMissingParentAndDuplicateBoneNames() throws Exception {
    Path missingParent =
        tempGeometry(
            "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{"
                + "\"description\":{\"identifier\":\"test:missing\",\"texture_width\":16,\"texture_height\":16},"
                + "\"bones\":[{\"name\":\"child\",\"parent\":\"ghost\",\"pivot\":[0,0,0],\"cubes\":[]}]"
                + "}]}");
    Path duplicate =
        tempGeometry(
            "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{"
                + "\"description\":{\"identifier\":\"test:duplicate\",\"texture_width\":16,\"texture_height\":16},"
                + "\"bones\":[{\"name\":\"same\",\"pivot\":[0,0,0],\"cubes\":[]},{\"name\":\"same\",\"pivot\":[0,0,0],\"cubes\":[]}]"
                + "}]}");

    var missingResult = parser.parse(missingParent, null);
    var duplicateResult = parser.parse(duplicate, null);

    assertFalse(missingResult.success());
    assertDiagnostic(missingResult, DiagnosticCodes.GEO_PARENT_NOT_FOUND);
    assertFalse(duplicateResult.success());
    assertDiagnostic(duplicateResult, DiagnosticCodes.GEO_DUPLICATE_BONE_NAME);
  }

  private static Path fixture(String name) {
    Path working = Path.of("").toAbsolutePath().normalize();
    Path root = working.getFileName().toString().equals("adapter-geckolib4") ? working.getParent() : working;
    return root.resolve("test-fixtures").resolve(name).resolve("geometry.geo.json");
  }

  private static Path tempGeometry(String json) throws Exception {
    Path path = Files.createTempFile("geometry", ".geo.json");
    Files.writeString(path, json);
    path.toFile().deleteOnExit();
    return path;
  }

  private static String geometry(String identifier, String bone) {
    return "{\"description\":{\"identifier\":\""
        + identifier
        + "\",\"texture_width\":16,\"texture_height\":16},\"bones\":[{\"name\":\""
        + bone
        + "\",\"pivot\":[0,0,0],\"cubes\":[]}]}";
  }

  private static String boneName(ModelIR model, String id) {
    return model.bones().stream()
        .filter(bone -> bone.id().value().equals(id))
        .findFirst()
        .orElseThrow()
        .name();
  }

  private static void assertDiagnostic(
      io.github.gabriel0liv.cpmconverter.diagnostics.Result<?> result, String code) {
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(diagnostic -> diagnostic.code().value().equals(code)),
        () -> result.diagnostics().all().toString());
  }
}
