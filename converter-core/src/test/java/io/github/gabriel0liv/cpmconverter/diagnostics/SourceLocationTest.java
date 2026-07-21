package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SourceLocationTest {
  @Test
  void windowsAndUnicodePathsAreNormalized() {
    assertEquals("models/player.json", SourcePath.of(Path.of("models\\player.json")).value());
    assertEquals("ação/model.json", SourcePath.of(Path.of("ação\\model.json")).value());
  }

  @Test
  void absolutePathsAreRejectedFromLogicalModel() {
    assertThrows(IllegalArgumentException.class, () -> new SourcePath("C:\\absolute\\model.json"));
  }
}
