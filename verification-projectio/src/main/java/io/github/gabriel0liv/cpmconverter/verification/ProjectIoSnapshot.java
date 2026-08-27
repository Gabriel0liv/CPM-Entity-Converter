package io.github.gabriel0liv.cpmconverter.verification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable converter-owned result of loading one archive through official ProjectIO. */
public record ProjectIoSnapshot(
    boolean loaded,
    int rootCount,
    int animationCount,
    int animationReferenceCount,
    List<ProjectIoElementSnapshot> elements,
    String failureType,
    String failureMessage) {
  public ProjectIoSnapshot {
    elements = List.copyOf(elements == null ? List.of() : elements);
    failureType = failureType == null ? "" : failureType;
    failureMessage = failureMessage == null ? "" : failureMessage;
  }

  public static ProjectIoSnapshot success(
      int rootCount, int animationCount, List<ProjectIoElementSnapshot> elements) {
    return success(rootCount, animationCount, -1, elements);
  }

  public static ProjectIoSnapshot success(
      int rootCount,
      int animationCount,
      int animationReferenceCount,
      List<ProjectIoElementSnapshot> elements) {
    return new ProjectIoSnapshot(
        true, rootCount, animationCount, animationReferenceCount, elements, "", "");
  }

  public static ProjectIoSnapshot failure(Throwable error) {
    return new ProjectIoSnapshot(
        false,
        0,
        0,
        -1,
        List.of(),
        error == null ? "" : error.getClass().getName(),
        normalize(error == null ? null : error.getMessage()));
  }

  public List<Long> storeIds() {
    return elements.stream()
        .map(ProjectIoElementSnapshot::storeId)
        .filter(storeId -> storeId > 0)
        .toList();
  }

  public List<Long> generatedStoreIds() {
    return elements.stream()
        .map(ProjectIoElementSnapshot::storeId)
        .filter(storeId -> storeId >= 1000)
        .toList();
  }

  public List<String> paths() {
    return elements.stream().map(ProjectIoElementSnapshot::path).toList();
  }

  public List<String> generatedPaths() {
    return elements.stream()
        .filter(element -> element.storeId() >= 1000)
        .map(ProjectIoElementSnapshot::path)
        .toList();
  }

  public Map<String, String> parentByGeneratedPath() {
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (ProjectIoElementSnapshot element : elements) {
      if (element.storeId() < 1000) continue;
      if (result.putIfAbsent(element.path(), element.parentPath()) != null) {
        throw new IllegalStateException("duplicate loaded generated path " + element.path());
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private static String normalize(String message) {
    return message == null ? "" : message.replaceAll("\\s+", " ").trim();
  }
}
