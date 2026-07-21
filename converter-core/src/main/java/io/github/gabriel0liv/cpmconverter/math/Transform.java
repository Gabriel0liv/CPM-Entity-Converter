package io.github.gabriel0liv.cpmconverter.math;

public record Transform(Vec3d translation, Quatd rotation, Vec3d scale) {
  public Transform {
    if (translation == null || rotation == null || scale == null)
      throw new IllegalArgumentException("transform fields");
  }

  public static Transform identity() {
    return new Transform(Vec3d.ZERO, Quatd.IDENTITY, new Vec3d(1, 1, 1));
  }

  public Mat4d composeMatrix(Transform child) {
    return matrix().multiply(child.matrix());
  }

  public Transform compose(Transform child) {
    if (Math.abs(scale.x() - scale.y()) > 1e-12
        || Math.abs(scale.y() - scale.z()) > 1e-12
        || Math.abs(child.scale.x() - child.scale.y()) > 1e-12
        || Math.abs(child.scale.y() - child.scale.z()) > 1e-12)
      throw new IllegalStateException("TRS composition may contain shear; use composeMatrix");
    return new Transform(
        translation.add(rotation.rotate(child.translation.hadamard(scale))),
        rotation.multiply(child.rotation),
        scale.hadamard(child.scale));
  }

  public Vec3d apply(Vec3d p) {
    return translation.add(rotation.rotate(p.hadamard(scale)));
  }

  public Mat4d matrix() {
    return Mat4d.trs(translation, rotation, scale);
  }

  /** General composition, retaining shear in a matrix instead of approximating it as TRS. */
  public Mat4d composeMatrix(Transform child, boolean retainShear) {
    return matrix().multiply(child.matrix());
  }
}
