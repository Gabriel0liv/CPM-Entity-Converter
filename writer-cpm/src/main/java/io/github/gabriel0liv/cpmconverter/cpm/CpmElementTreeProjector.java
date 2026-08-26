package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.ir.BoneIR;
import io.github.gabriel0liv.cpmconverter.ir.BoneId;
import io.github.gabriel0liv.cpmconverter.ir.CubeIR;
import io.github.gabriel0liv.cpmconverter.ir.ModelIR;
import io.github.gabriel0liv.cpmconverter.math.EulerAnglesZYX;
import io.github.gabriel0liv.cpmconverter.math.Quatd;
import io.github.gabriel0liv.cpmconverter.math.Transform;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Internal tree projection that preserves source hierarchy and cube ownership. */
final class CpmElementTreeProjector {
  private static final double EPSILON = 1e-12;
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  private final ModelIR model;
  private final Map<BoneId, BoneIR> bonesById;

  CpmElementTreeProjector(ModelIR model) {
    this.model = model;
    this.bonesById = new LinkedHashMap<>();
    for (BoneIR bone : model.bones()) bonesById.put(bone.id(), bone);
  }

  CpmElementV1 entityRoot(double modelScale, double verticalOffset) {
    List<CpmElementV1> projectedRoots = new ArrayList<>();
    for (BoneId rootId : model.roots()) projectedRoots.add(projectBone(bonesById.get(rootId)));
    return structural(
        ProjectionKey.entityRoot(),
        "entity_root",
        CpmElementKind.ENTITY_ROOT,
        new CpmTransformV1(
            new Vec3d(0, 24 + verticalOffset, 0),
            Vec3d.ZERO,
            new Vec3d(modelScale, modelScale, modelScale)),
        projectedRoots);
  }

  private CpmElementV1 projectBone(BoneIR bone) {
    List<CpmElementV1> children = new ArrayList<>();
    for (CubeIR cube : bone.cubes()) children.add(projectCube(cube, bone.renderOwnCubes()));
    for (BoneId childId : bone.children()) children.add(projectBone(bonesById.get(childId)));
    return structural(
        ProjectionKey.bone(bone.id()),
        bone.name(),
        CpmElementKind.BONE,
        projectTransform(bone.bind()),
        children);
  }

  private CpmElementV1 projectCube(CubeIR cube, boolean renderOwnCubes) {
    CpmElementV1 cubeElement = visibleCube(cube, !renderOwnCubes);
    if (isZero(cube.pivot()) && isIdentity(cube.rotation())) return cubeElement;
    return structural(
        ProjectionKey.cubeHelper(cube.id()),
        cube.id().value() + "_helper",
        CpmElementKind.CUBE_HELPER,
        new CpmTransformV1(cube.pivot(), rotationDegrees(cube.rotation()), ONE),
        List.of(cubeElement));
  }

  private CpmElementV1 visibleCube(CubeIR cube, boolean hidden) {
    return new CpmElementV1(
        ProjectionKey.cube(cube.id()),
        cube.id().value(),
        CpmElementKind.CUBE,
        CpmTransformV1.identity(),
        cube.origin(),
        cube.size(),
        cube.inflate(),
        cube.mirror(),
        true,
        true,
        hidden,
        cube.uv(),
        List.of());
  }

  private CpmElementV1 structural(
      ProjectionKey key,
      String name,
      CpmElementKind kind,
      CpmTransformV1 transform,
      List<CpmElementV1> children) {
    return new CpmElementV1(
        key,
        name,
        kind,
        transform,
        Vec3d.ZERO,
        Vec3d.ZERO,
        0,
        false,
        false,
        true,
        false,
        null,
        children);
  }

  private CpmTransformV1 projectTransform(Transform transform) {
    return new CpmTransformV1(
        transform.translation(), rotationDegrees(transform.rotation()), transform.scale());
  }

  private Vec3d rotationDegrees(Quatd rotation) {
    return EulerAnglesZYX.fromQuaternion(rotation).toDegrees();
  }

  private boolean isZero(Vec3d value) {
    return Math.abs(value.x()) <= EPSILON
        && Math.abs(value.y()) <= EPSILON
        && Math.abs(value.z()) <= EPSILON;
  }

  private boolean isIdentity(Quatd rotation) {
    Quatd normalized = rotation.normalized();
    return Math.abs(Math.abs(normalized.w()) - 1) <= EPSILON
        && Math.abs(normalized.x()) <= EPSILON
        && Math.abs(normalized.y()) <= EPSILON
        && Math.abs(normalized.z()) <= EPSILON;
  }
}
