package io.github.gabriel0liv.cpmconverter.cpm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import io.github.gabriel0liv.cpmconverter.ir.UvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class CpmProjectWriterV1Test {
  private static final LocalDateTime FIXED_ZIP_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);

  @Test
  void writesDeterministicStoredArchiveWithExactPngAndCanonicalStaticJson() throws Exception {
    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 1, 2, 3, 4};
    CpmStaticProjectV1 project = project(new BoxUvIR(4, 8), png);
    CpmStoreIdPlan ids = new CpmStoreIdAllocator().allocate(project).value();

    var first = new CpmProjectWriterV1().write(project, ids);
    var second = new CpmProjectWriterV1().write(project, ids);

    assertTrue(first.success(), () -> first.diagnostics().all().toString());
    assertTrue(second.success(), () -> second.diagnostics().all().toString());
    assertArrayEquals(first.value(), second.value());

    LinkedHashMap<String, ZipPayload> entries = readZip(first.value());
    assertEquals(List.of("config.json", "skin.png"), entries.keySet().stream().toList());
    assertEquals(ZipEntry.STORED, entries.get("config.json").method());
    assertEquals(ZipEntry.STORED, entries.get("skin.png").method());
    assertEquals(FIXED_ZIP_TIME, entries.get("config.json").time());
    assertEquals(FIXED_ZIP_TIME, entries.get("skin.png").time());
    assertArrayEquals(png, entries.get("skin.png").bytes());

    String config = new String(entries.get("config.json").bytes(), StandardCharsets.UTF_8);
    assertTrue(config.startsWith("{\"elements\":["));
    assertTrue(config.endsWith("\n"));
    assertFalse(config.contains("\r"));
    assertTrue(config.indexOf("\"elements\"") < config.indexOf("\"skinSize\""));
    assertTrue(config.indexOf("\"skinSize\"") < config.indexOf("\"skinType\""));
    assertTrue(config.indexOf("\"skinType\"") < config.indexOf("\"textures\""));
    assertTrue(config.indexOf("\"textures\"") < config.indexOf("\"version\""));
    assertTrue(config.contains("\"skinSize\":{\"x\":64,\"y\":64}"));
    assertTrue(config.contains("\"textures\":{\"skin\":{\"anim\":[],\"customGridSize\":false}}"));
    assertTrue(config.contains("\"storeID\":1000"));
    assertTrue(config.contains("\"storeID\":1001"));
    assertTrue(config.contains("\"mcScale\":0.25"));
    assertTrue(config.contains("\"mirror\":true"));
    assertTrue(config.contains("\"u\":4,\"v\":8"));
    assertTrue(config.indexOf("\"id\":\"head\"") < config.indexOf("\"id\":\"body\""));
  }

  @Test
  void serializesSignedPerFaceUvAsExactCpmIntegerEndpoints() throws Exception {
    LinkedHashMap<String, FaceUvIR> faces = new LinkedHashMap<>();
    faces.put("north", new FaceUvIR(8, 10, -4, 6));
    faces.put("up", new FaceUvIR(-2, 3, 5, -1));
    CpmStaticProjectV1 project = project(new PerFaceUvIR(faces), new byte[] {1});
    CpmStoreIdPlan ids = new CpmStoreIdAllocator().allocate(project).value();

    var result = new CpmProjectWriterV1().write(project, ids);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    String config =
        new String(readZip(result.value()).get("config.json").bytes(), StandardCharsets.UTF_8);
    assertTrue(
        config.contains(
            "\"north\":{\"autoUV\":false,\"ex\":4,\"ey\":16,\"rot\":\"0\",\"sx\":8,\"sy\":10}"));
    assertTrue(
        config.contains(
            "\"up\":{\"autoUV\":false,\"ex\":3,\"ey\":2,\"rot\":\"0\",\"sx\":-2,\"sy\":3}"));
  }

  @Test
  void rejectsFractionalUvInsteadOfSilentlyQuantizing() {
    CpmStaticProjectV1 project = project(new BoxUvIR(1.5, 2), new byte[] {1});
    CpmStoreIdPlan ids = new CpmStoreIdAllocator().allocate(project).value();

    var result = new CpmProjectWriterV1().write(project, ids);

    assertFalse(result.success());
    assertTrue(
        result.diagnostics().errors().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().value().equals(DiagnosticCodes.CPM_UV_UNREPRESENTABLE)));
  }

  private static CpmStaticProjectV1 project(UvIR uv, byte[] png) {
    ProjectionKey entityKey = ProjectionKey.entityRoot();
    ProjectionKey cubeKey = new ProjectionKey("CUBE:test");
    CpmElementV1 cube =
        new CpmElementV1(
            cubeKey,
            "cube",
            CpmElementKind.CUBE,
            CpmTransformV1.identity(),
            new Vec3d(-2, -4, -2),
            new Vec3d(4, 4, 4),
            0.25,
            true,
            true,
            true,
            false,
            uv,
            List.of());
    CpmElementV1 entityRoot =
        new CpmElementV1(
            entityKey,
            "entity_root",
            CpmElementKind.ENTITY_ROOT,
            new CpmTransformV1(new Vec3d(0, 24, 0), Vec3d.ZERO, new Vec3d(1, 1, 1)),
            Vec3d.ZERO,
            Vec3d.ZERO,
            0,
            false,
            false,
            true,
            false,
            null,
            List.of(cube));

    LinkedHashMap<ProjectionKey, CpmElementV1> targets = new LinkedHashMap<>();
    targets.put(entityKey, entityRoot);
    targets.put(cubeKey, cube);
    return new CpmStaticProjectV1(
        List.of(
            root(CpmVanillaPart.HEAD, List.of()),
            root(CpmVanillaPart.BODY, List.of(entityRoot)),
            root(CpmVanillaPart.LEFT_ARM, List.of()),
            root(CpmVanillaPart.RIGHT_ARM, List.of()),
            root(CpmVanillaPart.LEFT_LEG, List.of()),
            root(CpmVanillaPart.RIGHT_LEG, List.of())),
        List.of(new TextureIR("source.png", png, 64, 64, "fixture")),
        targets);
  }

  private static CpmRootV1 root(CpmVanillaPart part, List<CpmElementV1> children) {
    return new CpmRootV1(part, false, false, children);
  }

  private static LinkedHashMap<String, ZipPayload> readZip(byte[] archive) throws IOException {
    LinkedHashMap<String, ZipPayload> entries = new LinkedHashMap<>();
    try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        entries.put(
            entry.getName(),
            new ZipPayload(input.readAllBytes(), entry.getMethod(), entry.getTimeLocal()));
      }
    }
    return entries;
  }

  private record ZipPayload(byte[] bytes, int method, LocalDateTime time) {}
}
