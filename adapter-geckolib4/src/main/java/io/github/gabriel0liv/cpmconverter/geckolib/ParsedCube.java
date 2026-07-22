package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.SourceLocation;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.CubeId;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;

public record ParsedCube(
    CubeId id,
    BoneId boneId,
    Vec3d origin,
    Vec3d size,
    Vec3d pivot,
    Vec3d rotationDegrees,
    double inflate,
    boolean mirror,
    RawUvBoundary rawUv,
    SourceLocation source) {}
