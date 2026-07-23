package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmUvProjectorTest {
  private static final SourceLocation S =
      SourceLocation.of(new SourcePath("fixtures/test.geo.json"));

  @Test
  void projectsIntegralBoxAndPreservesMirror() {
    var c =
        new CubeIR(
            new CubeId("c"),
            new BoneId("b"),
            new Vec3d(0, 0, 0),
            new Vec3d(4, 5, 6),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            true,
            new BoxUvIR(4, 8),
            S);
    var r = new CpmUvProjector().project(c);
    assertTrue(r.success());
    assertEquals(new CpmBoxUvV1(4, 8), r.value());
    assertTrue(c.mirror());
  }

  @Test
  void projectsSignedPerFaceWithCanonicalRotation() {
    var m = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
    m.put(CubeFaceIR.DOWN, new FaceUvIR(8, 10, 4, -2));
    m.put(CubeFaceIR.NORTH, new FaceUvIR(1, 2, 3, 4));
    var c =
        new CubeIR(
            new CubeId("c"),
            new BoneId("b"),
            Vec3d.ZERO,
            new Vec3d(4, 5, 6),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            new PerFaceUvIR(m),
            S);
    var r = new CpmUvProjector().project(c);
    assertTrue(r.success());
    var uv = (CpmPerFaceUvV1) r.value();
    assertEquals(
        List.of(CpmCubeFace.NORTH, CpmCubeFace.DOWN), new ArrayList<>(uv.faces().keySet()));
    assertEquals(
        new CpmFaceUvV1(8, 10, 12, 8, CpmUvRotation.ROT_180, false),
        uv.faces().get(CpmCubeFace.DOWN));
  }

  @Test
  void rejectsFractionalBox() {
    var c =
        new CubeIR(
            new CubeId("c"),
            new BoneId("b"),
            Vec3d.ZERO,
            new Vec3d(4, 5, 6),
            Vec3d.ZERO,
            Quatd.IDENTITY,
            0,
            false,
            new BoxUvIR(1.5, 0),
            S);
    assertFalse(new CpmUvProjector().project(c).success());
  }
}
