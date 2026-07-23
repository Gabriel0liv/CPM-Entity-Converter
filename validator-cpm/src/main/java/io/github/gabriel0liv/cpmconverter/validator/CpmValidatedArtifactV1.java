package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;

public record CpmValidatedArtifactV1(CpmPersistedProjectV1 project, List<Object> animations, CpmArtifactInventory inventory, CpmValidationSummary summary) { public CpmValidatedArtifactV1 { animations = List.copyOf(animations); } }
