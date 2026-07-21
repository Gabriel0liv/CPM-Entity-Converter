package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Mat4dTest {
  @Test
  void trsInverseAndDefensiveCopy() {
    Mat4d matrix = Mat4d.trs(new Vec3d(2, 3, 4), Quatd.IDENTITY, new Vec3d(2, 2, 2));
    assertEquals(new Vec3d(4, 5, 6), matrix.transformPoint(new Vec3d(1, 1, 1)));
    assertEquals(Mat4d.identity(), matrix.multiply(matrix.inverseAffine()));
    double[] copy = matrix.valuesCopy();
    copy[0] = 99;
    assertNotEquals(99, matrix.valuesCopy()[0]);
  }

  @Test
  void singularMatrixFailsExplicitly() {
    assertThrows(
        IllegalStateException.class, () -> Mat4d.scale(new Vec3d(1, 0, 1)).inverseAffine());
  }

  @Test
  void matrixOperationsAreRowMajorColumnVectorAndContentBased() {
    Mat4d t = Mat4d.translation(new Vec3d(2, 3, 4));
    Mat4d s = Mat4d.scale(new Vec3d(2, 3, 4));
    assertEquals(new Vec3d(4, 6, 8), s.transformPoint(new Vec3d(2, 2, 2)));
    assertEquals(new Vec3d(2, 3, 4), t.transformPoint(Vec3d.ZERO));
    assertEquals(Vec3d.X, t.transformDirection(Vec3d.X));
    assertEquals(t, Mat4d.translation(new Vec3d(2, 3, 4)));
    assertEquals(t.hashCode(), Mat4d.translation(new Vec3d(2, 3, 4)).hashCode());
    assertEquals(t, t.transpose().transpose());
    assertTrue(t.isFinite());
  }

  @Test
  void inverseWorksWithRotationAndNonUniformScale() {
    Mat4d matrix =
        Mat4d.trs(new Vec3d(3, -2, 5), Quatd.fromEulerZYX(.2, -.4, .7), new Vec3d(2, 3, 4));
    Vec3d point = new Vec3d(-1, 2, 3);
    Vec3d restored = matrix.inverseAffine().transformPoint(matrix.transformPoint(point));
    assertEquals(point.x(), restored.x(), TestTolerance.MATRIX_EPSILON);
    assertEquals(point.y(), restored.y(), TestTolerance.MATRIX_EPSILON);
    assertEquals(point.z(), restored.z(), TestTolerance.MATRIX_EPSILON);
  }
}
