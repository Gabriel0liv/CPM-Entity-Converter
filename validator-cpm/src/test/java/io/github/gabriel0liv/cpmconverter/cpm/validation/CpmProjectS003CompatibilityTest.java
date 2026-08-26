package io.github.gabriel0liv.cpmconverter.cpm.validation;

import static io.github.gabriel0liv.cpmconverter.cpm.validation.CpmValidationProfile.EXISTING_V1;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpmProjectS003CompatibilityTest {
  private final CpmProjectValidator validator = new CpmProjectValidator();

  @ParameterizedTest
  @ValueSource(strings = {"M2", "M3", "M4", "M5"})
  void existingProfileAcceptsProjectIoCertifiedS003Artifacts(String caseName) throws IOException {
    Path artifact =
        repositoryRoot()
            .resolve("spikes")
            .resolve("minimal-cpmproject")
            .resolve("artifacts")
            .resolve(caseName + ".cpmproject");

    Result<CpmValidationReport> result = validator.validate(Files.readAllBytes(artifact), EXISTING_V1);

    assertTrue(result.success(), () -> caseName + ": " + result.diagnostics().all());
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("settings.gradle"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("cannot locate repository root from " + Path.of("").toAbsolutePath());
  }
}
