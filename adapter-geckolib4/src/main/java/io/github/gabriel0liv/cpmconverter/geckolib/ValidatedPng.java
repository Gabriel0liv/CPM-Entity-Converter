package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourcePath;

public record ValidatedPng(SourcePath source, int width, int height, long bytes, String sha256) {}
