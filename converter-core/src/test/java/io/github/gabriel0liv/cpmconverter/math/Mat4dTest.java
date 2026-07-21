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
}
