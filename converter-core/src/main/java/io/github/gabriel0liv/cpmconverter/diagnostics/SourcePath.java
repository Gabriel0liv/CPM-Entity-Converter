package io.github.gabriel0liv.cpmconverter.diagnostics;
import java.nio.file.Path;
public record SourcePath(String value) { public SourcePath { if (value == null || value.isBlank()) throw new IllegalArgumentException("source path is empty"); value = value.replace('\\','/'); while(value.startsWith("./")) value=value.substring(2); if (value.matches("^[A-Za-z]:/.*") || value.startsWith("/")) throw new IllegalArgumentException("absolute source path"); } public static SourcePath of(Path p){ return new SourcePath(p.toString()); } }
