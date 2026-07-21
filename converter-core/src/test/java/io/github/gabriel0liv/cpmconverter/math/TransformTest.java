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
    assertEquals(parent.matrix().multiply(child.matrix()), parent.composeMatrix(child, true));
  }

  @Test
  void identityApplyAndUniformComposition() {
    Transform transform = new Transform(new Vec3d(2, 0, 0), Quatd.IDENTITY, new Vec3d(2, 2, 2));
    assertEquals(new Vec3d(4, 0, 0), transform.apply(Vec3d.X));
    assertEquals(
        Transform.identity().matrix(), Transform.identity().composeMatrix(Transform.identity()));
    assertEquals(new Vec3d(4, 4, 4), transform.compose(transform).scale());
  }
}
