package io.github.gabriel0liv.cpmconverter.validator;

import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import java.util.*;

final class CpmPersistedUvTextureValidator {
  DiagnosticBag validate(CpmPersistedProjectV1 project, boolean skinEntryPresent,
      CpmPngMetadata pngMetadata, CpmArtifactLimits limits) {
    var bag = new DiagnosticBag();
    boolean textured = false;
    for (var element : project.elements()) {
      if (!element.texture()) continue;
      textured = true;
      if (!skinEntryPresent || pngMetadata == null) {
        bag = bag.add(error(DiagnosticCodes.PNG_DIMENSION_MISMATCH, element.pointer(),
            "textured element requires a valid skin.png"));
        continue;
      }
      bag = validateUv(element, project.skinSize(), bag);
    }
    if (skinEntryPresent && pngMetadata != null && !project.skinSize().equals(
        new CpmPersistedSize2i(pngMetadata.width(), pngMetadata.height()))
        && !project.texture().customGridSize()) {
      bag = bag.add(error(DiagnosticCodes.PNG_DIMENSION_MISMATCH, "/skin.png",
          "PNG dimensions do not match logical skin grid"));
    }
    return bag;
  }

  private DiagnosticBag validateUv(CpmPersistedElementV1 e, CpmPersistedSize2i grid,
      DiagnosticBag bag) {
    if (e.uv() instanceof CpmPersistedBoxUvV1 box) {
      if (box.u() < 0 || box.v() < 0) {
        return bag.add(error(DiagnosticCodes.UV_OUT_OF_BOUNDS, e.pointer() + "/uv",
            "box UV origin is negative"));
      }
      double width = 2d * (e.size().x() + e.size().z());
      double height = e.size().y() + e.size().z();
      if (box.u() + width > grid.x() || box.v() + height > grid.y()) {
        return bag.add(error(DiagnosticCodes.UV_OUT_OF_BOUNDS, e.pointer() + "/uv",
            "box UV footprint exceeds logical grid"));
      }
    } else if (e.uv() instanceof CpmPersistedPerFaceUvV1 perFace) {
      for (var entry : perFace.faces().entrySet()) {
        var face = entry.getValue();
        if (Math.min(face.sx(), face.ex()) < 0 || Math.min(face.sy(), face.ey()) < 0
            || Math.max(face.sx(), face.ex()) > grid.x()
            || Math.max(face.sy(), face.ey()) > grid.y()) {
          bag = bag.add(error(DiagnosticCodes.UV_OUT_OF_BOUNDS,
              e.pointer() + "/faceUV/" + entry.getKey().name().toLowerCase(Locale.ROOT),
              "face UV exceeds logical grid"));
        }
      }
    }
    return bag;
  }

  private static Diagnostic error(String code, String pointer, String message) {
    return new Diagnostic(Severity.ERROR, DiagnosticCode.fromCatalog(code),
        new SourceLocation(new SourcePath("config.json"), null, null, pointer, null), message,
        "repair the persisted texture or UV", null, null, new TreeMap<>());
  }
}
