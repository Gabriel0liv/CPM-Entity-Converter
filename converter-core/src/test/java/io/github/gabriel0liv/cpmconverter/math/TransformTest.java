package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TransformTest {
  @Test
  void parentChildAndShearPolicy() {
    Transform parent = new Transform(new Vec3d(1, 0, 0), Quatd.IDENTITY, new Vec3d(2, 1, 1));
    Transform child =
        new Transform(new Vec3d(0, 1, 0), Quatd.fromEulerZYX(0, 0, .5), new Vec3d(1, 2, 1));
    assertNotNull(parent.composeMatrix(child));
    assertThrows(IllegalStateException.class, () -> parent.compose(child));
  }
}
