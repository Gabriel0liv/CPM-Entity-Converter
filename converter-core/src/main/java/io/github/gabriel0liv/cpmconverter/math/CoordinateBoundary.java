package io.github.gabriel0liv.cpmconverter.math;
public final class CoordinateBoundary { private CoordinateBoundary(){} public static Vec3d geckoToCpmPosition(Vec3d v){return new Vec3d(-v.x(),-v.y(),v.z());} public static Vec3d geckoToCpmRotationDegrees(Vec3d v){return new Vec3d(-v.x(),-v.y(),v.z());} }
