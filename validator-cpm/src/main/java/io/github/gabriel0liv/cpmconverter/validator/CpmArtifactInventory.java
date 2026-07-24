package io.github.gabriel0liv.cpmconverter.validator;

import java.util.List;

public record CpmArtifactInventory(List<CpmArtifactEntry> entries) { public CpmArtifactInventory { entries = List.copyOf(entries); } }
