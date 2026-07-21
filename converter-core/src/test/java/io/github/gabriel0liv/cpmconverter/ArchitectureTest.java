package io.github.gabriel0liv.cpmconverter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.*;

class ArchitectureTest {
  @Test
  void coreHasNoUpstreamImports() throws Exception {
    for (Path module : productionModules()) {
      Path root = module.resolve("src/main/java");
      if (!Files.isDirectory(root)) continue;
      try (var stream = Files.walk(root)) {
        for (var path : stream.filter(Files::isRegularFile).toList()) {
          String source = Files.readString(path);
          assertFalse(
              source.contains("net.minecraft")
                  || source.contains("net.minecraftforge")
                  || source.contains("software.bernie.geckolib")
                  || source.contains("com.tom.cpm")
                  || source.contains("org.blockbench"),
              path.toString());
        }
      }
    }
  }

  @Test
  void productionDiagnosticsUseCatalogSymbols() throws Exception {
    for (Path module : productionModules()) {
      Path root = module.resolve("src/main/java");
      if (!Files.isDirectory(root)) continue;
      try (var stream = Files.walk(root)) {
        for (var path : stream.filter(Files::isRegularFile).toList()) {
          String source = Files.readString(path);
          assertFalse(
              source.matches("(?s).*Diagnostic\\.of\\([^;]*\\\"[A-Z][A-Z0-9_]+\\\".*"),
              path.toString());
        }
      }
    }
  }

  private static List<Path> productionModules() {
    Path project = Paths.get("..").toAbsolutePath().normalize();
    return List.of(
        project.resolve("converter-core"),
        project.resolve("converter-config"),
        project.resolve("adapter-geckolib4"),
        project.resolve("writer-cpm"),
        project.resolve("validator-cpm"),
        project.resolve("converter-cli"));
  }
}
