package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.ir.GeometryId;

public record GeometryParseRequest(GeometryId geometryId, GeometryParserLimits limits) {
  public GeometryParseRequest {
    if (limits == null) limits = GeometryParserLimits.defaults();
  }

  public static GeometryParseRequest defaults() {
    return new GeometryParseRequest(null, GeometryParserLimits.defaults());
  }
}
