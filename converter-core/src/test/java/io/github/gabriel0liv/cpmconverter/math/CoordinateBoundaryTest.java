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
}
