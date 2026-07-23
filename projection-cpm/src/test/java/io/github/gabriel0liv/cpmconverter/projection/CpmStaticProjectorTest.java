package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.config.*;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmStaticProjectorTest {
  @Test
  void emitsSixRootsAndProjectsAnchorHierarchy() {
    BoneId body = new BoneId("g/bone/0"), head = new BoneId("g/bone/1");
    SourceLocation source = SourceLocation.of(new SourcePath("fixtures/test.geo.json"));
    BoneIR b =
        new BoneIR(body, "body", null, List.of(head), Transform.identity(), List.of(), source);
    BoneIR h =
        new BoneIR(
            head,
            "head",
            body,
            List.of(),
            new Transform(new Vec3d(0, 8, 0), Quatd.IDENTITY, new Vec3d(1, 1, 1)),
            List.of(),
            source);
    ModelIR model =
        new ModelIR(
            new SourceDescriptor("fixtures/test.geo.json", "geo.json"),
            new GeometryId("g"),
            List.of(b, h),
            List.of(body),
            List.of(),
            List.of(new TextureIR("texture.png", 32, 32)),
            List.of());
    MappingDocumentV1 doc =
        new MappingDocumentV1(
            1,
            null,
            null,
            null,
            null,
            Map.of("body", "body"),
            Map.of(),
            Map.of(),
            null,
            Map.of(),
            null,
            List.of(),
            null);
    Result<SemanticRigMap> mapping = new MappingCompiler().compile(doc, new ModelIndex(model));
    assertTrue(mapping.success(), mapping.diagnostics().all().toString());
    Result<CpmStaticProjection> result = new CpmStaticProjector().project(model, mapping.value());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals(
        List.of(
            CpmVanillaRoot.HEAD,
            CpmVanillaRoot.BODY,
            CpmVanillaRoot.LEFT_ARM,
            CpmVanillaRoot.RIGHT_ARM,
            CpmVanillaRoot.LEFT_LEG,
            CpmVanillaRoot.RIGHT_LEG),
        result.value().project().roots().stream().map(CpmLogicalRootV1::root).toList());
    assertEquals(CpmNodeKey.class, result.value().index().boneTargets().get(body).key().getClass());
    assertTrue(result.value().index().boneTargets().containsKey(head));
  }

  @Test
  void rejectsRootPartitionAndMissingBody() {
    BoneId body = new BoneId("g/body");
    SourceLocation source = SourceLocation.of(new SourcePath("fixtures/test.geo.json"));
    ModelIR model =
        new ModelIR(
            new SourceDescriptor("fixtures/test.geo.json", "geo.json"),
            new GeometryId("g"),
            List.of(
                new BoneIR(body, "body", null, List.of(), Transform.identity(), List.of(), source)),
            List.of(body),
            List.of(),
            List.of(new TextureIR("texture.png", 32, 32)),
            List.of());
    MappingDocumentV1 doc = new MappingDocumentV1(1, Map.of(), Map.of(), null, null, List.of());
    Result<SemanticRigMap> mapping = new MappingCompiler().compile(doc, new ModelIndex(model));
    assertTrue(mapping.success());
    assertFalse(new CpmStaticProjector().project(model, mapping.value()).success());
  }
}
