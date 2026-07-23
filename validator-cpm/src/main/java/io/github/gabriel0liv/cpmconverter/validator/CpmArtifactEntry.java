package io.github.gabriel0liv.cpmconverter.validator;

public record CpmArtifactEntry(String name, int method, long localTime, long compressedSize, long uncompressedSize, long crc, boolean directory, int canonicalPosition) {}
