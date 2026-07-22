package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.CubeFaceIR;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.EnumMap;
import java.util.Map;

/** Derives the six GeckoLib box UV rectangles without applying mirror or CPM projection. */
public final class GeckoBoxUvLayout {
  private GeckoBoxUvLayout() {}

  public static Map<CubeFaceIR, FaceUvIR> derive(BoxUvIR uv, Vec3d cubeSize) {
    if (uv == null || cubeSize == null) throw new IllegalArgumentException("UV and size required");
    double x = Math.floor(cubeSize.x());
    double y = Math.floor(cubeSize.y());
    double z = Math.floor(cubeSize.z());
    double u = uv.u(), v = uv.v();
    var out = new EnumMap<CubeFaceIR, FaceUvIR>(CubeFaceIR.class);
    out.put(CubeFaceIR.NORTH, new FaceUvIR(u + z, v + z, x, y));
    out.put(CubeFaceIR.SOUTH, new FaceUvIR(u + z + x + z, v + z, x, y));
    out.put(CubeFaceIR.EAST, new FaceUvIR(u, v + z, z, y));
    out.put(CubeFaceIR.WEST, new FaceUvIR(u + z + x, v + z, z, y));
    out.put(CubeFaceIR.UP, new FaceUvIR(u + z, v, x, z));
    out.put(CubeFaceIR.DOWN, new FaceUvIR(u + z + x, v + z, x, -z));
    return java.util.Collections.unmodifiableMap(out);
  }
}
