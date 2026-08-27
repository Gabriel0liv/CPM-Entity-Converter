package io.github.gabriel0liv.cpmconverter.verification;

import java.util.List;

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

  public List<String> generatedPaths() {
    return elements.stream()
        .filter(element -> element.storeId() >= 1000)
        .map(ProjectIoElementSnapshot::path)
        .toList();
  }

  private static String normalize(String message) {
    return message == null ? "" : message.replaceAll("\\s+", " ").trim();
  }
}
