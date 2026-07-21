package io.github.gabriel0liv.cpmconverter.math;

public record Vec3d(double x, double y, double z) {
  public static final Vec3d ZERO = new Vec3d(0, 0, 0);
  public static final Vec3d X = new Vec3d(1, 0, 0);
  public static final Vec3d Y = new Vec3d(0, 1, 0);
  public static final Vec3d Z = new Vec3d(0, 0, 1);

  public Vec3d {
    if (!finite(x) || !finite(y) || !finite(z))
      throw new IllegalArgumentException("non-finite vector");
  }

  private static boolean finite(double v) {
    return Double.isFinite(v);
  }

  public Vec3d add(Vec3d o) {
    return new Vec3d(x + o.x, y + o.y, z + o.z);
  }

  public Vec3d subtract(Vec3d o) {
    return new Vec3d(x - o.x, y - o.y, z - o.z);
  }

  public Vec3d multiply(double s) {
    return new Vec3d(x * s, y * s, z * s);
  }

  public Vec3d hadamard(Vec3d o) {
    return new Vec3d(x * o.x, y * o.y, z * o.z);
  }

  public double dot(Vec3d o) {
    return x * o.x + y * o.y + z * o.z;
  }

  public Vec3d cross(Vec3d o) {
    return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
  }

  public double length() {
    return Math.sqrt(dot(this));
  }

  public Vec3d normalized() {
    double l = length();
    return l == 0 ? ZERO : multiply(1 / l);
  }
}
