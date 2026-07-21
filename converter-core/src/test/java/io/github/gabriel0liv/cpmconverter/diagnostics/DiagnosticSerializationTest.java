package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class DiagnosticSerializationTest {
  @Test
  void canonicalRepresentationIsStable() {
    Diagnostic diagnostic =
        new Diagnostic(
            Severity.WARNING,
            new DiagnosticCode(DiagnosticCodes.ANIM_OPTIONAL_CLIP_MISSING),
            new SourceLocation(new SourcePath("models/player.json"), 4, 2, "/states/idle", 18L),
            "clip is optional",
            "add the clip",
            "head",
            "idle",
            new TreeMap<>(Map.of("z", "last", "a", "first")));

    assertEquals(
        "severity=WARNING;code=ANIM_OPTIONAL_CLIP_MISSING;source=models/player.json;line=4;column=2;pointer=/states/idle;offset=18;bone=head;animation=idle;context.a=first;context.z=last;message=clip is optional;suggestion=add the clip",
        diagnostic.canonicalForm());
  }
}
