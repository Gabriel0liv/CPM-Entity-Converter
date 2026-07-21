package io.github.gabriel0liv.cpmconverter.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CoordinateBoundaryTest {
  @Test
  void geckoToCpmSigns() {
    assertEquals(new Vec3d(-1, -2, 3), CoordinateBoundary.geckoToCpmPosition(new Vec3d(1, 2, 3)));
    assertEquals(
        new Vec3d(-10, -20, 30),
        CoordinateBoundary.geckoToCpmRotationDegrees(new Vec3d(10, 20, 30)));
  }

  @Test
  void boundaryPreservesMagnitudeAndExplicitUnits() {
    Vec3d degrees = new Vec3d(180, -360, 720);
    assertEquals(new Vec3d(-180, 360, 720), CoordinateBoundary.geckoToCpmRotationDegrees(degrees));
    Vec3d radians = new Vec3d(Math.PI, -Math.PI / 2, 0);
    Vec3d converted = CoordinateBoundary.geckoToCpmRotationDegrees(radians);
    assertEquals(-Math.PI, converted.x(), TestTolerance.EPSILON);
    assertEquals(Math.PI / 2, converted.y(), TestTolerance.EPSILON);
  }
}
