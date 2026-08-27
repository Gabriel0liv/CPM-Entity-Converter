package io.github.gabriel0liv.cpmconverter.verification;

import io.github.gabriel0liv.cpmconverter.cpm.CpmElementV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmRootV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStaticProjectV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStoreIdPlan;
import io.github.gabriel0liv.cpmconverter.cpm.CpmVanillaPart;
import io.github.gabriel0liv.cpmconverter.ir.BoxUvIR;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Expected static semantics derived directly from the converter graph and store-ID plan. */
public final class ExpectedStaticSnapshot {
  private final Map<String, Element> generatedElementsByPath;
  private final Map<String, String> parentByGeneratedPath;
  private final Map<String, UvOriginSnapshot> boxUvOriginsByPath;
  private final Map<String, Boolean> perFaceUvPresenceByPath;
  private final Map<String, Map<String, FaceUvSnapshot>> perFaceUvByPath;

  private ExpectedStaticSnapshot(
      Map<String, Element> generatedElementsByPath,
      Map<String, String> parentByGeneratedPath,
      Map<String, UvOriginSnapshot> boxUvOriginsByPath,
      Map<String, Boolean> perFaceUvPresenceByPath,
      Map<String, Map<String, FaceUvSnapshot>> perFaceUvByPath) {
    this.generatedElementsByPath = immutableMap(generatedElementsByPath);
    this.parentByGeneratedPath = immutableMap(parentByGeneratedPath);
    this.boxUvOriginsByPath = immutableMap(boxUvOriginsByPath);
    this.perFaceUvPresenceByPath = immutableMap(perFaceUvPresenceByPath);
    this.perFaceUvByPath = immutableNestedMap(perFaceUvByPath);
  }

  public static ExpectedStaticSnapshot from(CpmStaticProjectV1 project, CpmStoreIdPlan storeIds) {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(storeIds, "storeIds");

    EnumMap<CpmVanillaPart, CpmRootV1> roots = new EnumMap<>(CpmVanillaPart.class);
    for (CpmRootV1 root : project.roots()) {
      if (roots.putIfAbsent(root.vanillaPart(), root) != null) {
        throw new IllegalArgumentException("duplicate CPM root " + root.vanillaPart());
      }
    }

    LinkedHashMap<String, Element> elements = new LinkedHashMap<>();
    LinkedHashMap<String, String> parents = new LinkedHashMap<>();
    LinkedHashMap<String, UvOriginSnapshot> boxUvOrigins = new LinkedHashMap<>();
    LinkedHashMap<String, Boolean> perFacePresence = new LinkedHashMap<>();
    LinkedHashMap<String, Map<String, FaceUvSnapshot>> perFaceUv = new LinkedHashMap<>();
    for (CpmVanillaPart part : CpmVanillaPart.values()) {
      CpmRootV1 root = roots.get(part);
      if (root == null) throw new IllegalArgumentException("missing CPM root " + part);
      String rootPath = part.name().toLowerCase(Locale.ROOT);
      for (CpmElementV1 child : root.children()) {
        append(
            child,
            rootPath,
            storeIds,
            elements,
            parents,
            boxUvOrigins,
            perFacePresence,
            perFaceUv);
      }
    }
    return new ExpectedStaticSnapshot(
        elements, parents, boxUvOrigins, perFacePresence, perFaceUv);
  }

  public Map<String, Element> generatedElementsByPath() {
    return generatedElementsByPath;
  }

  public Map<String, String> parentByGeneratedPath() {
    return parentByGeneratedPath;
  }

  public Map<String, UvOriginSnapshot> boxUvOriginsByPath() {
    return boxUvOriginsByPath;
  }

  public Map<String, Boolean> perFaceUvPresenceByPath() {
    return perFaceUvPresenceByPath;
  }

  public Map<String, Map<String, FaceUvSnapshot>> perFaceUvByPath() {
    return perFaceUvByPath;
  }

  private static void append(
      CpmElementV1 element,
      String parentPath,
      CpmStoreIdPlan storeIds,
      Map<String, Element> elements,
      Map<String, String> parents,
      Map<String, UvOriginSnapshot> boxUvOrigins,
      Map<String, Boolean> perFacePresence,
      Map<String, Map<String, FaceUvSnapshot>> perFaceUv) {
    String path = parentPath + "/" + element.name();
    Element expected =
        new Element(
            path,
            parentPath,
            storeIds.elementId(element.key()),
            vector(element.transform().translation()),
            vector(element.transform().rotationDegrees()),
            vector(element.transform().scale()));
    if (elements.putIfAbsent(path, expected) != null) {
      throw new IllegalArgumentException("duplicate generated path " + path);
    }
    parents.put(path, parentPath);

    boolean hasPerFaceUv = element.uv() instanceof PerFaceUvIR;
    perFacePresence.put(path, hasPerFaceUv);
    if (element.texture() && !hasPerFaceUv) {
      if (element.uv() instanceof BoxUvIR box) {
        boxUvOrigins.put(path, new UvOriginSnapshot(exactInt(box.u()), exactInt(box.v())));
      } else {
        boxUvOrigins.put(path, new UvOriginSnapshot(0, 0));
      }
    }
    if (element.uv() instanceof PerFaceUvIR perFace) {
      perFaceUv.put(path, expectedFaces(perFace));
    }

    for (CpmElementV1 child : element.children()) {
      append(
          child,
          path,
          storeIds,
          elements,
          parents,
          boxUvOrigins,
          perFacePresence,
          perFaceUv);
    }
  }

  private static Map<String, FaceUvSnapshot> expectedFaces(PerFaceUvIR perFace) {
    LinkedHashMap<String, FaceUvSnapshot> result = new LinkedHashMap<>();
    perFace.faces().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              FaceUvIR face = entry.getValue();
              int sx = exactInt(face.u());
              int sy = exactInt(face.v());
              int ex = exactInt(face.u() + face.width());
              int ey = exactInt(face.v() + face.height());
              result.put(entry.getKey(), new FaceUvSnapshot(sx, sy, ex, ey, "0", false));
            });
    return immutableMap(result);
  }

  private static int exactInt(double value) {
    if (!Double.isFinite(value)
        || value < Integer.MIN_VALUE
        || value > Integer.MAX_VALUE
        || value != Math.rint(value)) {
      throw new IllegalArgumentException("CPM V1 UV is not an exact integer: " + value);
    }
    return (int) value;
  }

  private static Vec3Snapshot vector(Vec3d value) {
    return new Vec3Snapshot(value.x(), value.y(), value.z());
  }

  private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  private static <K, K2, V> Map<K, Map<K2, V>> immutableNestedMap(
      Map<K, Map<K2, V>> source) {
    LinkedHashMap<K, Map<K2, V>> copy = new LinkedHashMap<>();
    source.forEach((key, value) -> copy.put(key, immutableMap(value)));
    return Collections.unmodifiableMap(copy);
  }

  public record Element(
      String path,
      String parentPath,
      long storeId,
      Vec3Snapshot position,
      Vec3Snapshot rotationDegrees,
      Vec3Snapshot scale) {}
}
