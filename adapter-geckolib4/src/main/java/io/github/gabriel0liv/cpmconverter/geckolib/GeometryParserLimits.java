package io.github.gabriel0liv.cpmconverter.geckolib;

/** NON_PRODUCTION? No: parser limits are production-safe T200 configuration. */
public record GeometryParserLimits(
    long maxBytes,
    int maxNestingDepth,
    int maxGeometries,
    int maxBones,
    int maxCubesPerBone,
    int maxTotalCubes,
    int maxHierarchyDepth,
    int maxStringLength) {
  public GeometryParserLimits {
    if (maxBytes <= 0
        || maxNestingDepth <= 0
        || maxGeometries <= 0
        || maxBones <= 0
        || maxCubesPerBone <= 0
        || maxTotalCubes <= 0
        || maxHierarchyDepth <= 0
        || maxStringLength <= 0) {
      throw new IllegalArgumentException("geometry parser limits must be positive");
    }
  }

  public static GeometryParserLimits defaults() {
    return new GeometryParserLimits(8_000_000, 64, 64, 4096, 4096, 16384, 256, 4096);
  }
}
