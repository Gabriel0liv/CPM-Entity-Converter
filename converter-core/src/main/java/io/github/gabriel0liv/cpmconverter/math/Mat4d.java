package io.github.gabriel0liv.cpmconverter.math;

import java.util.Arrays;

/** Immutable row-major 4x4 matrix with column-vector multiplication. TRS is T × R × S. */
public final class Mat4d {
  private final double[] values;

  public Mat4d(double[] values) {
    if (values == null
        || values.length != 16
        || Arrays.stream(values).anyMatch(v -> !Double.isFinite(v)))
      throw new IllegalArgumentException("invalid matrix");
    this.values = values.clone();
  }

  public static Mat4d identity() {
    double[] a = new double[16];
    for (int i = 0; i < 4; i++) a[i * 4 + i] = 1;
    return new Mat4d(a);
  }

  public static Mat4d translation(Vec3d p) {
    double[] a = identity().values;
    a[3] = p.x();
    a[7] = p.y();
    a[11] = p.z();
    return new Mat4d(a);
  }

  public static Mat4d scale(Vec3d s) {
    double[] a = identity().values;
    a[0] = s.x();
    a[5] = s.y();
    a[10] = s.z();
    return new Mat4d(a);
  }

  public static Mat4d rotation(Quatd q) {
    return q.toMatrix();
  }

  public static Mat4d trs(Vec3d translation, Quatd rotation, Vec3d scale) {
    return translation(translation).multiply(rotation(rotation)).multiply(scale(scale));
  }

  public Mat4d multiply(Mat4d b) {
    double[] o = new double[16];
    for (int r = 0; r < 4; r++)
      for (int c = 0; c < 4; c++)
        for (int k = 0; k < 4; k++) o[r * 4 + c] += values[r * 4 + k] * b.values[k * 4 + c];
    return new Mat4d(o);
  }

  public Mat4d transpose() {
    double[] o = new double[16];
    for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) o[c * 4 + r] = values[r * 4 + c];
    return new Mat4d(o);
  }

  public Vec3d transformPoint(Vec3d p) {
    return new Vec3d(
        values[0] * p.x() + values[1] * p.y() + values[2] * p.z() + values[3],
        values[4] * p.x() + values[5] * p.y() + values[6] * p.z() + values[7],
        values[8] * p.x() + values[9] * p.y() + values[10] * p.z() + values[11]);
  }

  public Vec3d transformDirection(Vec3d p) {
    return new Vec3d(
        values[0] * p.x() + values[1] * p.y() + values[2] * p.z(),
        values[4] * p.x() + values[5] * p.y() + values[6] * p.z(),
        values[8] * p.x() + values[9] * p.y() + values[10] * p.z());
  }

  public Mat4d inverseAffine() {
    double a = values[0],
        b = values[1],
        c = values[2],
        d = values[4],
        e = values[5],
        f = values[6],
        g = values[8],
        h = values[9],
        i = values[10];
    double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    if (Math.abs(det) < 1e-12) throw new IllegalStateException("singular affine matrix");
    double[] o = identity().values;
    o[0] = (e * i - f * h) / det;
    o[1] = (c * h - b * i) / det;
    o[2] = (b * f - c * e) / det;
    o[4] = (f * g - d * i) / det;
    o[5] = (a * i - c * g) / det;
    o[6] = (c * d - a * f) / det;
    o[8] = (d * h - e * g) / det;
    o[9] = (b * g - a * h) / det;
    o[10] = (a * e - b * d) / det;
    Vec3d t = new Vec3d(values[3], values[7], values[11]);
    Vec3d q =
        new Vec3d(
            o[0] * t.x() + o[1] * t.y() + o[2] * t.z(),
            o[4] * t.x() + o[5] * t.y() + o[6] * t.z(),
            o[8] * t.x() + o[9] * t.y() + o[10] * t.z());
    o[3] = -q.x();
    o[7] = -q.y();
    o[11] = -q.z();
    return new Mat4d(o);
  }

  public double[] valuesCopy() {
    return values.clone();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Mat4d m && Arrays.equals(values, m.values);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(values);
  }
}
