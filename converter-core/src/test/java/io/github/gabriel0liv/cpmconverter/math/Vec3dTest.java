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
}
