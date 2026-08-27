package io.github.gabriel0liv.cpmconverter.verification;

import com.tom.cpl.math.Vec3f;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.anim.AnimFrame;
import com.tom.cpm.shared.editor.anim.EditorAnim;
import com.tom.cpm.shared.editor.elements.ElementType;
import com.tom.cpm.shared.editor.elements.ModelElement;
import com.tom.cpm.shared.editor.project.ProjectFile;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.model.render.VanillaModelPart;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** Loads exact converter bytes through the pinned official CPM ProjectIO implementation. */
public final class ProjectIoHarness {
  public ProjectIoSnapshot load(byte[] archive) throws Exception {
    Path temporary = Files.createTempFile("t304-projectio-", ".cpmproject");
    try {
      Files.write(temporary, archive);
      return load(temporary);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public ProjectIoSnapshot load(Path archive) {
    try {
      CpmHeadlessEnvironment.initialize();
      ProjectFile project = new ProjectFile();
      project.load(archive.toFile()).join();
      Editor editor = CpmHeadlessEnvironment.newEditor();
      ProjectIO.loadProject(editor, project);
      return snapshot(editor);
    } catch (Throwable error) {
      return ProjectIoSnapshot.failure(rootCause(error));
    }
  }

  ProjectIoSnapshot snapshot(Editor editor) {
    List<ProjectIoElementSnapshot> elements = new ArrayList<>();
    for (ModelElement element : editor.elements) append(elements, element, "");
    return ProjectIoSnapshot.success(
        editor.elements.size(),
        editor.animations.size(),
        animationReferenceCount(editor),
        elements);
  }

  private void append(
      List<ProjectIoElementSnapshot> output, ModelElement element, String parentPath) {
    String displayName = stableName(element);
    String path = parentPath.isEmpty() ? displayName : parentPath + "/" + displayName;
    output.add(
        new ProjectIoElementSnapshot(
            path,
            parentPath,
            element.name == null ? "" : element.name,
            String.valueOf(element.type),
            element.storeID,
            element.texture,
            element.textureSize,
            element.u,
            element.v,
            vector(element.pos),
            vector(element.rotation),
            scaleVector(element, element.scale),
            scaleVector(element, element.meshScale),
            element.faceUV != null));
    for (ModelElement child : element.children) append(output, child, path);
  }

  private String stableName(ModelElement element) {
    if (element.typeData instanceof VanillaModelPart vanillaPart) return vanillaPart.getName();
    return element.name == null ? "" : element.name;
  }

  private int animationReferenceCount(Editor editor) {
    try {
      Field framesField = EditorAnim.class.getDeclaredField("frames");
      Field componentsField = AnimFrame.class.getDeclaredField("components");
      framesField.setAccessible(true);
      componentsField.setAccessible(true);

      int references = 0;
      for (EditorAnim animation : editor.animations) {
        Object rawFrames = framesField.get(animation);
        if (!(rawFrames instanceof List<?> frames)) return -1;
        for (Object frame : frames) {
          Object rawComponents = componentsField.get(frame);
          if (!(rawComponents instanceof Map<?, ?> components)) return -1;
          references += components.size();
        }
      }
      return references;
    } catch (ReflectiveOperationException | RuntimeException error) {
      return -1;
    }
  }

  private Vec3Snapshot scaleVector(ModelElement element, Vec3f value) {
    if (value == null && element.type == ElementType.ROOT_PART) {
      return new Vec3Snapshot(1, 1, 1);
    }
    return vector(value);
  }

  private Vec3Snapshot vector(Vec3f value) {
    if (value == null) throw new IllegalStateException("ProjectIO element vector is null");
    return new Vec3Snapshot(value.x, value.y, value.z);
  }

  private Throwable rootCause(Throwable error) {
    Throwable current = error;
    while ((current instanceof CompletionException || current instanceof ExecutionException)
        && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }
}
