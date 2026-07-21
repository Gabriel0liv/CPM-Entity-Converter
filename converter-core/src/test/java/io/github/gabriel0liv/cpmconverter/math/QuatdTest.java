package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QuatdTest {
  @Test
  void rotationsAndInverse() {
    Quatd q = Quatd.fromEulerZYX(Math.PI / 2, 0, 0);
    assertEquals(0, q.rotate(Vec3d.Y).y(), TestTolerance.EPSILON);
    Vec3d restored = q.multiply(q.inverse()).rotate(Vec3d.Y);
    assertEquals(Vec3d.Y.x(), restored.x(), TestTolerance.EPSILON);
    assertThrows(IllegalStateException.class, () -> new Quatd(0, 0, 0, 0).inverse());
  }

  @Test
  void axesAndNonCommutativeComposition() {
    Quatd x = Quatd.fromEulerZYX(Math.PI / 2, 0, 0);
    Quatd y = Quatd.fromEulerZYX(0, Math.PI / 2, 0);
    assertVectorEquals(Vec3d.Z, x.rotate(Vec3d.Y));
    assertNotEquals(x.multiply(y), y.multiply(x));
    Quatd identity = x.multiply(x.inverse());
    assertEquals(Quatd.IDENTITY.w(), identity.w(), TestTolerance.EPSILON);
    assertEquals(Quatd.IDENTITY.x(), identity.x(), TestTolerance.EPSILON);
    assertVectorEquals(new Vec3d(0, 1, 0), x.conjugate().rotate(Vec3d.Z));
    assertThrows(IllegalStateException.class, () -> new Quatd(0, 0, 0, 0).normalized());
  }

  @Test
  void degreeWindingIsNotEncodedByQuaternion() {
    Quatd oneTurn = Quatd.fromEulerZYX(0, 0, Math.toRadians(360));
    assertVectorEquals(Vec3d.X, oneTurn.rotate(Vec3d.X));
    assertNotEquals(0, Math.toRadians(720));
  }

  private static void assertVectorEquals(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x(), actual.x(), TestTolerance.EPSILON);
    assertEquals(expected.y(), actual.y(), TestTolerance.EPSILON);
    assertEquals(expected.z(), actual.z(), TestTolerance.EPSILON);
  }
}
