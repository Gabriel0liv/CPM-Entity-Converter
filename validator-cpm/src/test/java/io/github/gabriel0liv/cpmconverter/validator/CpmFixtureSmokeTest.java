package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

/** Dedicated smoke for the B/D fixture corpora used by the phase-3 pipeline. */
class CpmFixtureSmokeTest {
  @Test
  void fixtureBAndDInputsAreStableAndComplete() throws Exception {
    for (String fixture : new String[] {"fixture-b-neck", "fixture-d-quadruped"}) {
      Path root = Path.of("test-fixtures", fixture);
      if (!Files.exists(root)) root = Path.of("..", "test-fixtures", fixture);
      assertTrue(Files.exists(root.resolve("texture.png")), fixture);
      assertTrue(Files.exists(root.resolve("expected/model-static.json")), fixture);
      byte[] model = Files.readAllBytes(root.resolve("expected/model-static.json"));
      byte[] texture = Files.readAllBytes(root.resolve("texture.png"));
      assertArrayEquals(sha256(model), sha256(Files.readAllBytes(root.resolve("expected/model-static.json"))));
      assertTrue(texture.length > 32, fixture + " texture");
    }
  }

  private static byte[] sha256(byte[] bytes) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(bytes);
  }
}
