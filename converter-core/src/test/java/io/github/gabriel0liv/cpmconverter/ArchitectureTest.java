package io.github.gabriel0liv.cpmconverter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.*;

class ArchitectureTest {
  @Test
  void coreHasNoUpstreamImports() throws Exception {
    var root = Paths.get("src/main/java");
    try (var s = Files.walk(root)) {
      for (var p : s.filter(Files::isRegularFile).toList()) {
        String x = Files.readString(p);
        assertFalse(
            x.contains("net.minecraft")
                || x.contains("net.minecraftforge")
                || x.contains("software.bernie.geckolib")
                || x.contains("com.tom.cpm")
                || x.contains("org.blockbench"),
            p.toString());
      }
    }
  }

  @Test
  void productionDiagnosticsUseCatalogSymbols() throws Exception {
    try (var stream = Files.walk(Paths.get("src/main/java"))) {
      for (var path : stream.filter(Files::isRegularFile).toList()) {
        String source = Files.readString(path);
        assertFalse(
            source.matches("(?s).*Diagnostic\\.of\\([^;]*\\\"[A-Z][A-Z0-9_]+\\\".*"),
            path.toString());
      }
    }
  }
}
