package io.github.gabriel0liv.cpmconverter.geckolib4;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeckoGeometryTextureIntegrationTest {
  @Test
  void attachesFixtureATextureUsingGridDeclaredBySelectedGeometry() throws Exception {
    Path fixture = Path.of("..", "test-fixtures", "fixture-a-humanoid").normalize();
    Path geometry = fixture.resolve("geometry.geo.json");
    Path texture = fixture.resolve("texture.png");
    byte[] sourceBytes = Files.readAllBytes(texture);

    var result = new GeckoGeometryParser().parse(geometry, "cpm:a", texture);

    assertTrue(result.success(), () -> result.diagnostics().all().toString());
    assertEquals(1, result.value().textures().size());
    var parsedTexture = result.value().textures().get(0);
    assertEquals(32, parsedTexture.width());
    assertEquals(32, parsedTexture.height());
    assertArrayEquals(sourceBytes, parsedTexture.pngBytes());
  }
}
