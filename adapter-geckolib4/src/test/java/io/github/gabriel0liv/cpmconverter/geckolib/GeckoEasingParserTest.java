package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gabriel0liv.cpmconverter.diagnostics.*;
import io.github.gabriel0liv.cpmconverter.ir.*;
import org.junit.jupiter.api.Test;

class GeckoEasingParserTest {
  @Test
  void mapsNamesAndArguments() throws Exception {
    var node = new ObjectMapper().readTree("{\"easing\":\"EaSeInBaCk\",\"easingArgs\":[1.2,0.35]}");
    var result =
        new GeckoEasingParser()
            .parse(node, new SourcePath("a.animation.json"), "/k", "idle", "head", "rotation");
    assertTrue(result.success());
    assertEquals(EasingKindIR.EASE_IN_BACK, result.value().kind());
    assertEquals(java.util.List.of(1.2, 0.35), result.value().args());
  }

  @Test
  void rejectsUnknownAndAcceptsArgsWithoutName() throws Exception {
    var mapper = new ObjectMapper();
    assertFalse(
        new GeckoEasingParser()
            .parse(
                mapper.readTree("{\"easing\":\"my_mod:curve\"}"),
                new SourcePath("a"),
                "/k",
                "c",
                "b",
                "r")
            .success());
    var linear =
        new GeckoEasingParser()
            .parse(
                mapper.readTree("{\"easingArgs\":[\"1.2\"]}"),
                new SourcePath("a"),
                "/k",
                "c",
                "b",
                "r");
    assertTrue(linear.success());
    assertEquals(EasingKindIR.LINEAR, linear.value().kind());
  }
}
