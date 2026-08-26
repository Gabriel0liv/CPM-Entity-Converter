package io.github.gabriel0liv.cpmconverter.cpm;

import io.github.gabriel0liv.cpmconverter.diagnostics.Diagnostic;
import io.github.gabriel0liv.cpmconverter.diagnostics.DiagnosticCodes;
import io.github.gabriel0liv.cpmconverter.diagnostics.Result;
import io.github.gabriel0liv.cpmconverter.diagnostics.Severity;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.TextureIR;
import io.github.gabriel0liv.cpmconverter.ir.UvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Serializes the static CPM V1 graph into a deterministic .cpmproject ZIP payload. */
public final class CpmProjectWriterV1 {
  private static final Vec3d ONE = new Vec3d(1, 1, 1);

  public Result<byte[]> write(CpmStaticProjectV1 project, CpmStoreIdPlan storeIds) {
    if (project == null || storeIds == null) {
      return failure(DiagnosticCodes.INTERNAL_ERROR, "project and storeIds are required");
    }

    try {
      LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
      entries.put("config.json", CanonicalJsonWriter.write(projectMap(project, storeIds)));
      if (!project.textures().isEmpty()) {
        if (project.textures().size() != 1) {
          return failure(
              DiagnosticCodes.INTERNAL_ERROR,
              "static CPM V1 writer requires at most one skin texture");
        }
        entries.put("skin.png", project.textures().get(0).pngBytes());
      }
      return Result.success(DeterministicZipWriter.write(entries));
    } catch (UvRepresentationException exception) {
      return failure(DiagnosticCodes.CPM_UV_UNREPRESENTABLE, exception.getMessage());
    } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
      String message = exception.getMessage();
      return failure(
          DiagnosticCodes.INTERNAL_ERROR,
          message == null ? "CPM V1 project serialization failed" : message);
    }
  }

  private Map<String, Object> projectMap(CpmStaticProjectV1 project, CpmStoreIdPlan storeIds) {
    LinkedHashMap<String, Object> config = new LinkedHashMap<>();
    config.put("version", 1);
    config.put("elements", rootMaps(project, storeIds));
    if (!project.textures().isEmpty()) addTextureConfig(config, project.textures().get(0));
    return config;
  }

  private List<Map<String, Object>> rootMaps(CpmStaticProjectV1 project, CpmStoreIdPlan storeIds) {
    EnumMap<CpmVanillaPart, CpmRootV1> roots = new EnumMap<>(CpmVanillaPart.class);
    for (CpmRootV1 root : project.roots()) {
      if (roots.putIfAbsent(root.vanillaPart(), root) != null) {
        throw new IllegalArgumentException("duplicate CPM root " + root.vanillaPart().name());
      }
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (CpmVanillaPart part : CpmVanillaPart.values()) {
      CpmRootV1 root = roots.get(part);
      if (root == null) throw new IllegalArgumentException("missing CPM root " + part.name());
      result.add(rootMap(root, storeIds));
    }
    return List.copyOf(result);
  }

  private Map<String, Object> rootMap(CpmRootV1 root, CpmStoreIdPlan storeIds) {
    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
    String id = root.vanillaPart().name().toLowerCase(Locale.ROOT);
    value.put("id", id);
    value.put("pos", vector(Vec3d.ZERO));
    value.put("rotation", vector(Vec3d.ZERO));
    value.put("show", root.showVanillaGeometry());
    value.put("showInEditor", true);
    value.put("locked", false);
    value.put("dup", false);
    value.put("disableVanillaAnim", root.disableVanillaAnim());
    value.put("name", id);
    value.put("nameColor", 0);
    if (!root.children().isEmpty()) value.put("children", elementMaps(root.children(), storeIds));
    return value;
  }

  private List<Map<String, Object>> elementMaps(
      List<CpmElementV1> elements, CpmStoreIdPlan storeIds) {
    List<Map<String, Object>> result = new ArrayList<>(elements.size());
    for (CpmElementV1 element : elements) result.add(elementMap(element, storeIds));
    return List.copyOf(result);
  }

  private Map<String, Object> elementMap(CpmElementV1 element, CpmStoreIdPlan storeIds) {
    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
    value.put("name", element.name());
    value.put("show", element.show());
    value.put("texture", element.texture());
    value.put("textureSize", 1);
    value.put("offset", vector(element.offset()));
    value.put("pos", vector(element.transform().translation()));
    value.put("rotation", vector(element.transform().rotationDegrees()));
    value.put("size", vector(element.size()));
    value.put("rscale", vector(element.transform().scale()));
    value.put("scale", vector(ONE));
    value.put("color", "ffffff");
    value.put("mirror", element.mirror());
    value.put("mcScale", element.mcScale());
    value.put("glow", false);
    value.put("recolor", false);
    value.put("hidden", element.hidden());
    value.put("singleTex", false);
    value.put("extrude", false);
    value.put("locked", false);
    value.put("nameColor", 0);
    value.put("storeID", storeIds.elementId(element.key()));
    addUv(value, element.uv());
    if (!element.children().isEmpty())
      value.put("children", elementMaps(element.children(), storeIds));
    return value;
  }

  private void addUv(Map<String, Object> value, UvIR uv) {
    if (uv == null) {
      value.put("u", 0);
      value.put("v", 0);
      return;
    }
    if (uv instanceof BoxUvIR box) {
      value.put("u", exactUvInt(box.u(), "box u"));
      value.put("v", exactUvInt(box.v(), "box v"));
      return;
    }
    if (uv instanceof PerFaceUvIR perFace) {
      value.put("u", 0);
      value.put("v", 0);
      LinkedHashMap<String, Object> faces = new LinkedHashMap<>();
      for (Map.Entry<String, FaceUvIR> face : perFace.faces().entrySet()) {
        faces.put(face.getKey(), faceMap(face.getValue(), face.getKey()));
      }
      value.put("faceUV", faces);
      return;
    }
    throw new IllegalArgumentException("unsupported UV representation " + uv.getClass().getName());
  }

  private Map<String, Object> faceMap(FaceUvIR face, String faceName) {
    int sx = exactUvInt(face.u(), faceName + " sx");
    int sy = exactUvInt(face.v(), faceName + " sy");
    int ex = exactUvInt(face.u() + face.width(), faceName + " ex");
    int ey = exactUvInt(face.v() + face.height(), faceName + " ey");

    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
    value.put("sx", sx);
    value.put("sy", sy);
    value.put("ex", ex);
    value.put("ey", ey);
    value.put("rot", "0");
    value.put("autoUV", false);
    return value;
  }

  private int exactUvInt(double value, String field) {
    if (!Double.isFinite(value)
        || value < Integer.MIN_VALUE
        || value > Integer.MAX_VALUE
        || value != Math.rint(value)) {
      throw new UvRepresentationException(
          field + "=" + Double.toString(value) + " is not exactly representable by CPM V1 int UV");
    }
    return (int) value;
  }

  private Map<String, Object> vector(Vec3d vector) {
    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
    value.put("x", vector.x());
    value.put("y", vector.y());
    value.put("z", vector.z());
    return value;
  }

  private void addTextureConfig(Map<String, Object> config, TextureIR texture) {
    LinkedHashMap<String, Object> size = new LinkedHashMap<>();
    size.put("x", texture.width());
    size.put("y", texture.height());
    config.put("skinSize", size);
    config.put("skinType", "default");

    LinkedHashMap<String, Object> skin = new LinkedHashMap<>();
    skin.put("anim", List.of());
    skin.put("customGridSize", false);
    LinkedHashMap<String, Object> textures = new LinkedHashMap<>();
    textures.put("skin", skin);
    config.put("textures", textures);
  }

  private <T> Result<T> failure(String code, String message) {
    return Result.failure(Diagnostic.of(Severity.ERROR, code, message));
  }

  private static final class UvRepresentationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private UvRepresentationException(String message) {
      super(message);
    }
  }
}
