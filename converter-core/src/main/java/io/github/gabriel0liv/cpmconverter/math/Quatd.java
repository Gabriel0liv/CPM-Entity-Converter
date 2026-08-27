package io.github.gabriel0liv.cpmconverter.math;

public record Quatd(double w, double x, double y, double z) {
  public static final Quatd IDENTITY = new Quatd(1, 0, 0, 0);

  public Quatd {
    if (!Double.isFinite(w) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
      throw new IllegalArgumentException("non-finite quaternion");
  }

  public Quatd normalized() {
    double n = Math.sqrt(w * w + x * x + y * y + z * z);
    if (n == 0) throw new IllegalStateException("zero quaternion");
    return new Quatd(w / n, x / n, y / n, z / n);
  }

  public Quatd conjugate() {
    return new Quatd(w, -x, -y, -z);
  }

  public Quatd inverse() {
    double n = w * w + x * x + y * y + z * z;
    if (n == 0) throw new IllegalStateException("zero quaternion");
    return new Quatd(w / n, -x / n, -y / n, -z / n);
  }

  public Quatd multiply(Quatd q) {
    return new Quatd(
        w * q.w - x * q.x - y * q.y - z * q.z,
        w * q.x + x * q.w + y * q.z - z * q.y,
        w * q.y - x * q.z + y * q.w + z * q.x,
        w * q.z + x * q.y - y * q.x + z * q.w);
  }

  public static Quatd fromEulerZYX(double rx, double ry, double rz) {
    double cx = Math.cos(rx / 2),
        sx = Math.sin(rx / 2),
        cy = Math.cos(ry / 2),
        sy = Math.sin(ry / 2),
        cz = Math.cos(rz / 2),
        sz = Math.sin(rz / 2);
    return new Quatd(
            cz * cy * cx + sz * sy * sx,
            cz * cy * sx - sz * sy * cx,
            cz * sy * cx + sz * cy * sx,
            sz * cy * cx - cz * sy * sx)
        .normalized();
  }

  /** Converts the rotation block of a row-major matrix to a unit quaternion. */
  public static Quatd fromRotationMatrix(Mat4d matrix) {
    if (matrix == null) throw new IllegalArgumentException("matrix");
    double[] m = matrix.valuesCopy();
    double trace = m[0] + m[5] + m[10];
    double w;
    double x;
    double y;
    double z;

    if (trace > 0) {
      double s = Math.sqrt(trace + 1) * 2;
      w = 0.25 * s;
      x = (m[9] - m[6]) / s;
      y = (m[2] - m[8]) / s;
      z = (m[4] - m[1]) / s;
    } else if (m[0] > m[5] && m[0] > m[10]) {
      double s = Math.sqrt(1 + m[0] - m[5] - m[10]) * 2;
      w = (m[9] - m[6]) / s;
      x = 0.25 * s;
      y = (m[1] + m[4]) / s;
      z = (m[2] + m[8]) / s;
    } else if (m[5] > m[10]) {
      double s = Math.sqrt(1 + m[5] - m[0] - m[10]) * 2;
      w = (m[2] - m[8]) / s;
      x = (m[1] + m[4]) / s;
      y = 0.25 * s;
      z = (m[6] + m[9]) / s;
    } else {
      double s = Math.sqrt(1 + m[10] - m[0] - m[5]) * 2;
      w = (m[4] - m[1]) / s;
      x = (m[2] + m[8]) / s;
      y = (m[6] + m[9]) / s;
      z = 0.25 * s;
    }
    return new Quatd(w, x, y, z).normalized();
  }

  public Vec3d rotate(Vec3d v) {
    Quatd r = new Quatd(0, v.x(), v.y(), v.z());
    Quatd o = multiply(r).multiply(inverse());
    return new Vec3d(o.x, o.y, o.z);
  }

  public Mat4d toMatrix() {
    Quatd q = normalized();
    double w = q.w(), x = q.x(), y = q.y(), z = q.z();
    return new Mat4d(
        new double[] {
          1 - 2 * (y * y + z * z),
          2 * (x * y - z * w),
          2 * (x * z + y * w),
          0,
          2 * (x * y + z * w),
          1 - 2 * (x * x + z * z),
          2 * (y * z - x * w),
          0,
          2 * (x * z - y * w),
          2 * (y * z + x * w),
          1 - 2 * (x * x + y * y),
          0,
          0,
          0,
          0,
          1
        });
  }
}
