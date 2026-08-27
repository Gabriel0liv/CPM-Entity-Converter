package io.github.gabriel0liv.cpmconverter.verification;

import io.github.gabriel0liv.cpmconverter.cpm.CpmElementV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmRootV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStaticProjectV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmStoreIdPlan;
import io.github.gabriel0liv.cpmconverter.cpm.CpmVanillaPart;
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

  private ExpectedStaticSnapshot(
      Map<String, Element> generatedElementsByPath, Map<String, String> parentByGeneratedPath) {
    this.generatedElementsByPath =
        Collections.unmodifiableMap(new LinkedHashMap<>(generatedElementsByPath));
    this.parentByGeneratedPath =
        Collections.unmodifiableMap(new LinkedHashMap<>(parentByGeneratedPath));
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
    for (CpmVanillaPart part : CpmVanillaPart.values()) {
      CpmRootV1 root = roots.get(part);
      if (root == null) throw new IllegalArgumentException("missing CPM root " + part);
      String rootPath = part.name().toLowerCase(Locale.ROOT);
      for (CpmElementV1 child : root.children()) {
        append(child, rootPath, storeIds, elements, parents);
      }
    }
    return new ExpectedStaticSnapshot(elements, parents);
  }

  public Map<String, Element> generatedElementsByPath() {
    return generatedElementsByPath;
  }

  public Map<String, String> parentByGeneratedPath() {
    return parentByGeneratedPath;
  }

  private static void append(
      CpmElementV1 element,
      String parentPath,
      CpmStoreIdPlan storeIds,
      Map<String, Element> elements,
      Map<String, String> parents) {
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
    for (CpmElementV1 child : element.children()) {
      append(child, path, storeIds, elements, parents);
    }
  }

  private static Vec3Snapshot vector(Vec3d value) {
    return new Vec3Snapshot(value.x(), value.y(), value.z());
  }

  public record Element(
      String path,
      String parentPath,
      long storeId,
      Vec3Snapshot position,
      Vec3Snapshot rotationDegrees,
      Vec3Snapshot scale) {}
}
