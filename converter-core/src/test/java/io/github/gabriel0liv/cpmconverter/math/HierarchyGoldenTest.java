package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Golden values derived independently with a scalar matrix calculation. */
class HierarchyGoldenTest {
  private static final double EPSILON = 1e-9;

  @Test
  void bodyNeckHeadHornUsesParentTimesLocalWithPivotsAndShear() {
    Transform body =
        new Transform(
            new Vec3d(1, 2, 3), Quatd.fromEulerZYX(0, 0, Math.toRadians(30)), new Vec3d(2, 1, 1));
    Transform neck =
        new Transform(
            new Vec3d(0.5, -1, 2),
            Quatd.fromEulerZYX(Math.toRadians(20), 0, 0),
            new Vec3d(1, 3, 1));
    Transform head =
        new Transform(
            new Vec3d(-1, 0.75, 0.5),
            Quatd.fromEulerZYX(0, Math.toRadians(-15), 0),
            new Vec3d(0.5, 1, 2));
    Transform horn =
        new Transform(
            new Vec3d(0.25, 1.2, -0.4),
            Quatd.fromEulerZYX(0, 0, Math.toRadians(10)),
            new Vec3d(1, 1, 1));

    Mat4d world = body.composeMatrix(neck).multiply(head.matrix()).multiply(horn.matrix());
    double[][] expected = {
      {0.600837996952, -1.537227291106, -0.566209382619, -1.587975871839},
      {0.861820547289, 2.327093251983, -1.089848942402, 5.793689556259},
      {0.297931241413, 0.989355749744, 1.815346742381, 6.774926745246},
      {0, 0, 0, 1}
    };
    for (int row = 0; row < 4; row++) {
      for (int column = 0; column < 4; column++) {
        assertEquals(expected[row][column], world.get(row, column), EPSILON);
      }
    }
    assertVectorEquals(
        new Vec3d(-1.436541782670, 4.910104865097, 7.966970811529),
        world.transformPoint(new Vec3d(0.4, -0.2, 0.7)));
    assertVectorEquals(
        new Vec3d(3.392187887854, -4.337290427878, -0.773106886885),
        world.transformDirection(new Vec3d(1, -2, 0.5)));
    assertVectorEquals(
        new Vec3d(0.4, -0.2, 0.7),
        world.inverseAffine().transformPoint(world.transformPoint(new Vec3d(0.4, -0.2, 0.7))));
  }

  private static void assertVectorEquals(Vec3d expected, Vec3d actual) {
    assertEquals(expected.x(), actual.x(), EPSILON);
    assertEquals(expected.y(), actual.y(), EPSILON);
    assertEquals(expected.z(), actual.z(), EPSILON);
  }
}
