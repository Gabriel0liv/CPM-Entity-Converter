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
          assertFalse(
              source.matches("(?s).*new\\s+DiagnosticCode\\s*\\(\\s*\\\"[A-Z][A-Z0-9_]+\\\".*"),
              path.toString());
        }
      }
    }
  }

  @Test
  void productionDoesNotDeclareLocalDiagnosticCodeFactories() throws Exception {
    for (Path module : productionModules()) {
      Path root = module.resolve("src/main/java");
      if (!Files.isDirectory(root)) continue;
      try (var stream = Files.walk(root)) {
        for (Path path : stream.filter(Files::isRegularFile).toList()) {
          String source = Files.readString(path);
          if (path.getFileName().toString().equals("DiagnosticCode.java")
              || path.getFileName().toString().equals("DiagnosticCodes.java")) {
            continue;
          }
          assertFalse(
              source.matches("(?s).*DiagnosticCode\\s+(?:[A-Za-z0-9_]+)\\s*\\([^)]*\\).*"),
              path.toString());
        }
      }
    }
  }

  private static List<Path> productionModules() {
    Path project = projectRoot();
    return List.of(
        project.resolve("converter-core"),
        project.resolve("converter-config"),
        project.resolve("adapter-geckolib4"),
        project.resolve("writer-cpm"),
        project.resolve("validator-cpm"),
        project.resolve("converter-cli"));
  }

  private static Path projectRoot() {
    Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    return current == null ? Paths.get(System.getProperty("user.dir")).toAbsolutePath() : current;
  }
}
