package io.github.gabriel0liv.cpmconverter.validator;

public record CpmPersistedVec3(double x, double y, double z) { public CpmPersistedVec3 { if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)) throw new IllegalArgumentException("finite vector required"); } }
