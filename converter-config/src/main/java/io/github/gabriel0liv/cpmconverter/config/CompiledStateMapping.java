package io.github.gabriel0liv.cpmconverter.config;

import io.github.gabriel0liv.cpmconverter.ir.ClipId;

public record CompiledStateMapping(
    ClipId clip, String mode, boolean optional, Integer requestedFps) {}
