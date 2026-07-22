package io.github.gabriel0liv.cpmconverter.geckolib;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import java.nio.file.Path;
import java.util.*;

/** Builds the static ModelIR boundary from parsed geometry and validated texture metadata. */
public final class GeckoStaticModelAssembler {
  public Result<ModelIR> assemble(
      ParsedGeometry geometry, Path texturePath, StaticModelAssemblyRequest request) {
    if (geometry == null || texturePath == null)
      return Result.failure(invalid("geometry and texture are required", null));
    var png =
        new PngTextureValidator()
            .validate(
                texturePath, request == null ? PngValidationRequest.defaults() : request.png());
    if (!png.success()) return Result.failure(png.diagnostics());
    var diagnostics = png.diagnostics();
    var texture = png.value();
    if (texture.width() != geometry.textureWidth()
        || texture.height() != geometry.textureHeight()) {
      var context = new TreeMap<String, String>();
      context.put("geometryWidth", Integer.toString(geometry.textureWidth()));
      context.put("geometryHeight", Integer.toString(geometry.textureHeight()));
      context.put("pngWidth", Integer.toString(texture.width()));
      context.put("pngHeight", Integer.toString(texture.height()));
      diagnostics =
          diagnostics.add(
              new Diagnostic(
                  Severity.ERROR,
                  DiagnosticCode.fromCatalog(DiagnosticCodes.PNG_DIMENSION_MISMATCH),
                  SourceLocation.of(texture.source()),
                  "PNG dimensions do not match geometry",
                  "provide the matching texture or correct geometry dimensions",
                  null,
                  null,
                  context));
      return Result.failure(diagnostics);
    }
    var cubes = new HashMap<CubeId, CubeIR>();
    var bones = new ArrayList<BoneIR>();
    for (ParsedBone bone : geometry.bones()) {
      var converted = new ArrayList<CubeIR>();
      for (ParsedCube cube : bone.cubes()) {
        var uv =
            new GeckoUvDecoder()
                .decode(cube.rawUv(), cube, geometry.textureWidth(), geometry.textureHeight());
        diagnostics = diagnostics.addAll(uv.diagnostics());
        if (!uv.success()) continue;
        var origin =
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(
                -(cube.origin().x() + cube.size().x()),
                -(cube.origin().y() + cube.size().y()),
                cube.origin().z());
        var pivot =
            new io.github.gabriel0liv.cpmconverter.math.Vec3d(
                -cube.pivot().x(), -cube.pivot().y(), cube.pivot().z());
        var rotation =
            io.github.gabriel0liv.cpmconverter.math.Quatd.fromEulerZYX(
                Math.toRadians(-cube.rotationDegrees().x()),
                Math.toRadians(-cube.rotationDegrees().y()),
                Math.toRadians(cube.rotationDegrees().z()));
        var ir =
            new CubeIR(
                cube.id(),
                cube.boneId(),
                origin,
                cube.size(),
                pivot,
                rotation,
                cube.inflate(),
                cube.mirror(),
                uv.value(),
                cube.source());
        converted.add(ir);
        cubes.put(cube.id(), ir);
      }
      bones.add(
          new BoneIR(
              bone.id(),
              bone.sourceName(),
              bone.parent(),
              bone.children(),
              bone.bindLocal(),
              converted,
              bone.source()));
    }
    if (diagnostics.hasErrors()) return Result.failure(diagnostics);
    var model =
        new ModelIR(
            new SourceDescriptor(geometry.source().value(), "geckolib-4.4.9/geometry-1.12.0"),
            geometry.geometryId(),
            bones,
            geometry.roots(),
            List.of(),
            List.of(new TextureIR(texture.source().value(), texture.width(), texture.height())),
            geometry.unsupportedFeatures());
    var validator = new io.github.gabriel0liv.cpmconverter.ir.ModelIrValidator().validate(model);
    diagnostics = diagnostics.addAll(validator);
    return validator.hasErrors() ? Result.failure(diagnostics) : Result.success(model, diagnostics);
  }

  private static Diagnostic invalid(String message, SourceLocation location) {
    return new Diagnostic(
        Severity.ERROR,
        DiagnosticCode.fromCatalog(DiagnosticCodes.IR_INVALID_VALUE),
        location,
        message,
        "provide valid static model inputs",
        null,
        null,
        new TreeMap<>());
  }
}
