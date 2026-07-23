package io.github.gabriel0liv.cpmconverter.projection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.config.*;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmProjectionIndexTest {
  @Test
  void indexUsesModelOrder() {
    var s = SourceLocation.of(new SourcePath("fixtures/test.geo.json"));
    var body = new BoneId("body");
    var head = new BoneId("head");
    var arm = new BoneId("arm");
    var b = new BoneIR(body, "body", null, List.of(head, arm), Transform.identity(), List.of(), s);
    var h = new BoneIR(head, "head", body, List.of(), Transform.identity(), List.of(), s);
    var a = new BoneIR(arm, "arm", body, List.of(), Transform.identity(), List.of(), s);
    var model =
        new ModelIR(
            new SourceDescriptor("fixtures/test.geo.json", "geo"),
            new GeometryId("g"),
            List.of(b, h, a),
            List.of(body),
            List.of(),
            List.of(new TextureIR("texture.png", 32, 32)),
            List.of());
    var doc =
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
    var map = new MappingCompiler().compile(doc, new ModelIndex(model));
    assertTrue(map.success());
    var result = new CpmStaticProjector().project(model, map.value());
    assertTrue(result.success(), result.diagnostics().all().toString());
    assertEquals(
        List.of(body, head, arm), new ArrayList<>(result.value().index().boneTargets().keySet()));
  }
}
