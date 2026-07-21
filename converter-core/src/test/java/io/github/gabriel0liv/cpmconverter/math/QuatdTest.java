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
}
