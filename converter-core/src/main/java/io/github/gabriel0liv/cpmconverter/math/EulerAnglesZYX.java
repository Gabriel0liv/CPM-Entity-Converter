package io.github.gabriel0liv.cpmconverter.math;

public record EulerAnglesZYX(double x, double y, double z) {
  private static final double GIMBAL_EPSILON = 1e-10;

  public Quatd toQuaternion() {
    return Quatd.fromEulerZYX(x, y, z);
  }

  public static EulerAnglesZYX fromDegrees(double x, double y, double z) {
    return new EulerAnglesZYX(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
  }

  /** Returns one principal ZYX Euler representation of the supplied orientation. */
  public static EulerAnglesZYX fromQuaternion(Quatd quaternion) {
    if (quaternion == null) throw new IllegalArgumentException("quaternion");
    double[] m = quaternion.normalized().toMatrix().valuesCopy();
    double sinY = clamp(-m[8], -1, 1);
    double y = Math.asin(sinY);
    double cosY = Math.cos(y);
    double x;
    double z;

    if (Math.abs(cosY) > GIMBAL_EPSILON) {
      x = Math.atan2(m[9], m[10]);
      z = Math.atan2(m[4], m[0]);
    } else {
      z = 0;
      x = sinY >= 0 ? Math.atan2(m[1], m[2]) : Math.atan2(-m[1], -m[2]);
    }
    return new EulerAnglesZYX(x, y, z);
  }

  public Vec3d toDegrees() {
    return new Vec3d(Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z));
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
