package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AuthoredPivotResolverTest {
  @Test
  void resolvesChildBeforeParentByMemoizedRecursion() {
    var s = SourceLocation.of(new SourcePath("fixtures/test.geo.json"));
    var body = new BoneId("body");
    var neck = new BoneId("neck");
    var head = new BoneId("head");
    var h =
        new BoneIR(
            head,
            "head",
            neck,
            List.of(),
            new Transform(new Vec3d(0, -2, 1), Quatd.IDENTITY, new Vec3d(1, 1, 1)),
            List.of(),
            s);
    var n =
        new BoneIR(
            neck,
            "neck",
            body,
            List.of(head),
            new Transform(new Vec3d(0, -4, 0), Quatd.IDENTITY, new Vec3d(1, 1, 1)),
            List.of(),
            s);
    var b =
        new BoneIR(
            body,
            "body",
            null,
            List.of(neck),
            new Transform(new Vec3d(1, 2, 3), Quatd.IDENTITY, new Vec3d(1, 1, 1)),
            List.of(),
            s);
    var r = new AuthoredPivotResolver(List.of(h, n, b), body).resolve();
    assertTrue(r.success());
    assertEquals(new Vec3d(1, -4, 4), r.value().get(head));
  }
}
