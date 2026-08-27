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
  void positiveNinetyDegreeAxesMatchColumnVectorZyxConvention() {
    assertVecNear(Vec3d.Z, Quatd.fromEulerZYX(Math.PI / 2, 0, 0).rotate(Vec3d.Y), 1e-9);
    assertVecNear(Vec3d.X, Quatd.fromEulerZYX(0, Math.PI / 2, 0).rotate(Vec3d.Z), 1e-9);
    assertVecNear(Vec3d.Y, Quatd.fromEulerZYX(0, 0, Math.PI / 2).rotate(Vec3d.X), 1e-9);
  }

  @Test
  void zyxOrderIsNonCommutativeAndExplicit() {
    Quatd zyx = Quatd.fromEulerZYX(Math.PI / 2, Math.PI / 2, 0);
    assertVecNear(Vec3d.X, zyx.rotate(Vec3d.Y), 1e-9);

    Quatd reversed =
        Quatd.fromEulerZYX(Math.PI / 2, 0, 0).multiply(Quatd.fromEulerZYX(0, Math.PI / 2, 0));
    assertFalse(near(zyx.rotate(Vec3d.Y), reversed.rotate(Vec3d.Y), 1e-9));
  }

  @Test
  void rotationMatrixRoundTripKeepsOrientation() {
    Quatd source = Quatd.fromEulerZYX(Math.toRadians(33), Math.toRadians(-41), Math.toRadians(79));
    Quatd restored = Quatd.fromRotationMatrix(source.toMatrix());

    assertVecNear(source.rotate(Vec3d.X), restored.rotate(Vec3d.X), 1e-9);
    assertVecNear(source.rotate(Vec3d.Y), restored.rotate(Vec3d.Y), 1e-9);
    assertVecNear(source.rotate(Vec3d.Z), restored.rotate(Vec3d.Z), 1e-9);
  }

  @Test
  void eulerExtractionRoundTripSurvivesGimbalLock() {
    Quatd source = Quatd.fromEulerZYX(Math.toRadians(20), Math.toRadians(90), Math.toRadians(30));
    EulerAnglesZYX extracted = EulerAnglesZYX.fromQuaternion(source);
    Quatd restored = extracted.toQuaternion();

    assertVecNear(source.rotate(Vec3d.X), restored.rotate(Vec3d.X), 1e-9);
    assertVecNear(source.rotate(Vec3d.Y), restored.rotate(Vec3d.Y), 1e-9);
    assertVecNear(source.rotate(Vec3d.Z), restored.rotate(Vec3d.Z), 1e-9);
  }

  private static boolean near(Vec3d a, Vec3d b, double epsilon) {
    return Math.abs(a.x() - b.x()) <= epsilon
        && Math.abs(a.y() - b.y()) <= epsilon
        && Math.abs(a.z() - b.z()) <= epsilon;
  }

  private static void assertVecNear(Vec3d expected, Vec3d actual, double epsilon) {
    assertEquals(expected.x(), actual.x(), epsilon);
    assertEquals(expected.y(), actual.y(), epsilon);
    assertEquals(expected.z(), actual.z(), epsilon);
  }
}
