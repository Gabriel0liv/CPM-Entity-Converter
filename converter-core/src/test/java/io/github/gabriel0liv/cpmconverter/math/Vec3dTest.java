package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Vec3dTest {
  @Test
  void axesAndProducts() {
    assertEquals(1, Vec3d.X.dot(Vec3d.X), TestTolerance.EPSILON);
    assertEquals(0, Vec3d.X.dot(Vec3d.Y), TestTolerance.EPSILON);
    assertEquals(Vec3d.Z, Vec3d.X.cross(Vec3d.Y));
    assertEquals(Vec3d.X, Vec3d.X.normalized());
  }

  @Test
  void completeVectorAlgebraAndFiniteBoundary() {
    Vec3d a = new Vec3d(1, 2, 3);
    Vec3d b = new Vec3d(-2, 4, 1);
    assertEquals(new Vec3d(-1, 6, 4), a.add(b));
    assertEquals(new Vec3d(3, -2, 2), a.subtract(b));
    assertEquals(new Vec3d(2, 4, 6), a.multiply(2));
    assertEquals(new Vec3d(-2, 8, 3), a.hadamard(b));
    assertEquals(3, a.dot(Vec3d.Z), TestTolerance.EPSILON);
    assertEquals(new Vec3d(-10, -7, 8), a.cross(b));
    assertEquals(0, Vec3d.ZERO.normalized().length(), TestTolerance.EPSILON);
    assertTrue(a.isFinite());
    assertThrows(IllegalArgumentException.class, () -> new Vec3d(Double.NaN, 0, 0));
  }
}
