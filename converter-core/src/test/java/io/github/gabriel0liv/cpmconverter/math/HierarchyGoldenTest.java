package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Independent non-identity golden transform for body/neck/head/horn inheritance. */
class HierarchyGoldenTest {
  @Test
  void worldTransformPropagatesPivotedHierarchy() {
    Transform body =
        new Transform(new Vec3d(1, 0, 0), Quatd.fromEulerZYX(0, 0, .1), new Vec3d(1, 1, 1));
    Transform neck =
        new Transform(new Vec3d(0, 2, 0), Quatd.fromEulerZYX(0, .2, 0), new Vec3d(1, 1, 1));
    Transform head =
        new Transform(new Vec3d(0, 1, 0), Quatd.fromEulerZYX(.3, 0, 0), new Vec3d(1, 1, 1));
    Transform horn = new Transform(new Vec3d(0, 1, 0), Quatd.IDENTITY, new Vec3d(1, 1, 1));
    Mat4d world = body.composeMatrix(neck).multiply(head.matrix()).multiply(horn.matrix());
    Vec3d expected =
        body.matrix()
            .multiply(neck.matrix())
            .multiply(head.matrix())
            .multiply(horn.matrix())
            .transformPoint(Vec3d.ZERO);
    assertEquals(expected, world.transformPoint(Vec3d.ZERO));
    Vec3d restored = world.inverseAffine().inverseAffine().transformPoint(Vec3d.ZERO);
    assertEquals(expected.x(), restored.x(), TestTolerance.MATRIX_EPSILON);
    assertEquals(expected.y(), restored.y(), TestTolerance.MATRIX_EPSILON);
    assertEquals(expected.z(), restored.z(), TestTolerance.MATRIX_EPSILON);
  }
}
