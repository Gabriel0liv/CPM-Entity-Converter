package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TransformDecompositionTest {
  @Test
  void decomposesAndRecomposesNonUniformTrs() {
    Mat4d original =
        Mat4d.trs(
            new Vec3d(3.25, -7.5, 11),
            Quatd.fromEulerZYX(Math.toRadians(23), Math.toRadians(-37), Math.toRadians(61)),
            new Vec3d(2, 3, 4));

    Transform decomposed = original.decomposeTrs(1e-10);

    assertMatrixNear(original, decomposed.matrix(), 1e-9);
    assertEquals(new Vec3d(3.25, -7.5, 11), decomposed.translation());
    assertVectorNear(new Vec3d(2, 3, 4), decomposed.scale(), 1e-12);
  }

  @Test
  void preservesWorldTransformWhenReparented() {
    Mat4d newParentWorld =
        Mat4d.trs(
            new Vec3d(4, 2, -3),
            Quatd.fromEulerZYX(Math.toRadians(15), Math.toRadians(35), Math.toRadians(-20)),
            new Vec3d(1, 1, 1));
    Mat4d originalWorld =
        Mat4d.trs(
            new Vec3d(-6, 5, 9),
            Quatd.fromEulerZYX(Math.toRadians(-31), Math.toRadians(12), Math.toRadians(48)),
            new Vec3d(1.5, 1.5, 1.5));

    Mat4d newLocal = newParentWorld.inverseAffine().multiply(originalWorld);
    Transform localTrs = newLocal.decomposeTrs(1e-10);

    assertMatrixNear(originalWorld, newParentWorld.multiply(localTrs.matrix()), 1e-9);
  }

  @Test
  void rejectsShearInsteadOfSilentlyApproximatingIt() {
    Mat4d shear =
        new Mat4d(
            new double[] {
              1, 0.25, 0, 0,
              0, 1, 0, 0,
              0, 0, 1, 0,
              0, 0, 0, 1
            });

    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> shear.decomposeTrs(1e-10));
    assertTrue(error.getMessage().toLowerCase().contains("shear"));
  }

  @Test
  void rejectsSingularScaleInsteadOfProducingUndefinedRotation() {
    Mat4d singular = Mat4d.trs(Vec3d.ZERO, Quatd.IDENTITY, new Vec3d(1, 0, 1));

    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> singular.decomposeTrs(1e-10));

    assertTrue(error.getMessage().toLowerCase().contains("singular"));
  }

  @Test
  void handlesReflectionWithoutChangingTheMatrix() {
    Mat4d original =
        Mat4d.trs(
            new Vec3d(1, 2, 3),
            Quatd.fromEulerZYX(Math.toRadians(17), Math.toRadians(29), Math.toRadians(-11)),
            new Vec3d(-2, 3, 4));

    Transform decomposed = original.decomposeTrs(1e-10);

    assertMatrixNear(original, decomposed.matrix(), 1e-9);
    assertTrue(
        decomposed.scale().x() < 0 || decomposed.scale().y() < 0 || decomposed.scale().z() < 0);
  }

  @Test
  void rejectsNonAffineBottomRow() {
    Mat4d perspectiveLike =
        new Mat4d(new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0.01, 0, 0, 1});

    assertThrows(IllegalStateException.class, () -> perspectiveLike.decomposeTrs(1e-10));
  }

  private static void assertMatrixNear(Mat4d expected, Mat4d actual, double epsilon) {
    double[] a = expected.valuesCopy();
    double[] b = actual.valuesCopy();
    for (int i = 0; i < 16; i++) assertEquals(a[i], b[i], epsilon, "matrix index " + i);
  }

  private static void assertVectorNear(Vec3d expected, Vec3d actual, double epsilon) {
    assertEquals(expected.x(), actual.x(), epsilon, "vector x");
    assertEquals(expected.y(), actual.y(), epsilon, "vector y");
    assertEquals(expected.z(), actual.z(), epsilon, "vector z");
  }
}
