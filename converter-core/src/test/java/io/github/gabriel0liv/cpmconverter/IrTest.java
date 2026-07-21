package io.github.gabriel0liv.cpmconverter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.*;
import io.github.gabriel0liv.cpmconverter.math.*;
import java.util.*;
import org.junit.jupiter.api.*;

class IrTest {
  @Test
  void validatesDanglingReferences() {
    var b =
        new BoneIR(new BoneId("body"), "body", null, List.of(), Transform.identity(), List.of());
    var m =
        new ModelIR(
            new SourceDescriptor("fixture/model", "geo"),
            List.of(b),
            List.of(new BoneId("missing")),
            List.of(),
            List.of());
    assertTrue(new ModelIrValidator().validate(m).hasErrors());
  }

  @Test
  void preservesOrderAndCopies() {
    var x = new ArrayList<BoneId>();
    x.add(new BoneId("a"));
    var m =
        new ModelIR(
            new SourceDescriptor("fixture/model", "geo"), List.of(), x, List.of(), List.of());
    x.add(new BoneId("b"));
    assertEquals(1, m.roots().size());
  }
}
