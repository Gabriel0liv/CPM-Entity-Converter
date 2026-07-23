package io.github.gabriel0liv.cpmconverter.projection;

import io.github.gabriel0liv.cpmconverter.math.*;

public final class QuaternionToEulerZYX {
  private QuaternionToEulerZYX() {}

  public static Vec3d decompose(Quatd input) {
    Quatd q = input.normalized();
    double w = q.w(), x = q.x(), y = q.y(), z = q.z();
    double rx = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
    double ry = Math.asin(Math.max(-1, Math.min(1, 2 * (w * y - z * x))));
    double rz = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));
    return new Vec3d(
        clean(Math.toDegrees(rx)), clean(Math.toDegrees(ry)), clean(Math.toDegrees(rz)));
  }

  private static double clean(double v) {
    return v == 0.0d ? 0.0d : v;
  }
}
