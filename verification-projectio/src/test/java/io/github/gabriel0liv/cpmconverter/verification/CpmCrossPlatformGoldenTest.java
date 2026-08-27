package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CpmCrossPlatformGoldenTest {
  private final CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

  @Test
  void emittedBytesMatchCurrentArchitectureGoldens() throws Exception {
    Properties expected = loadExpectedHashes();

    assertEquals(Set.copyOf(CurrentFixturePipeline.FIXTURES), expected.stringPropertyNames());
    for (String fixture : CurrentFixturePipeline.FIXTURES) {
      assertEquals(expected.getProperty(fixture), pipeline.generate(fixture).sha256(), fixture);
    }
  }

  private Properties loadExpectedHashes() throws IOException {
    Path manifest = Path.of("expected-artifact-hashes.properties");
    assertTrue(Files.isRegularFile(manifest), "expected-artifact-hashes.properties is missing");

    Properties expected = new Properties();
    try (InputStream input = Files.newInputStream(manifest)) {
      expected.load(input);
    }
    return expected;
  }
}
