package org.example.cpm;
import org.junit.jupiter.api.*; import static org.junit.jupiter.api.Assertions.*; import java.nio.file.*;
class ArchitectureTest { @Test void coreHasNoUpstreamImports() throws Exception {var root=Paths.get("src/main/java");try(var s=Files.walk(root)){for(var p:s.filter(Files::isRegularFile).toList()){String x=Files.readString(p);assertFalse(x.contains("net.minecraft")||x.contains("net.minecraftforge")||x.contains("software.bernie.geckolib")||x.contains("com.tom.cpm")||x.contains("org.blockbench"),p.toString());}}}}
