package io.github.gabriel0liv.cpmconverter.geckolib;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GeckoOracleParityTest {
  private static final Set<String> MATCH =
      Set.of(
          "PREPOST-001",
          "PREPOST-002",
          "PREPOST-003",
          "PREPOST-004",
          "LERP-001",
          "LERP-002",
          "EASE-001",
          "EASE-002",
          "EASE-003",
          "EASE-005",
          "EASE-006",
          "EASE-007",
          "MOLANG-001",
          "MOLANG-002",
          "PLAYBACK-001",
          "PLAYBACK-002",
          "PLAYBACK-003",
          "LENGTH-001",
          "LENGTH-002",
          "KEYFRAME-001",
          "KEYFRAME-003",
          "KEYFRAME-004",
          "ROTATION-001",
          "ROTATION-002",
          "ROTATION-003",
          "POSITION-001",
          "SCALE-001",
          "SCALE-002");
  private static final Set<String> STRICT =
      Set.of(
          "PREPOST-005",
          "EASE-004",
          "MOLANG-003",
          "PLAYBACK-006",
          "LENGTH-003",
          "LENGTH-004",
          "KEYFRAME-002");
  private static final Set<String> DEFERRED = Set.of("PLAYBACK-004", "PLAYBACK-005");

  @Test
  void matrixHasExactlyTheFrozen37Fixtures() throws Exception {
    Set<String> all = new HashSet<>();
    all.addAll(MATCH);
    all.addAll(STRICT);
    all.addAll(DEFERRED);
    assertEquals(28, MATCH.size());
    assertEquals(7, STRICT.size());
    assertEquals(2, DEFERRED.size());
    assertEquals(37, all.size());
    var manifest =
        new ObjectMapper()
            .readTree(
                Path.of("..", "spikes", "geckolib-animation-semantics", "fixture-manifest.json")
                    .toFile());
    Set<String> names = new HashSet<>();
    manifest.get("fixtures").forEach(n -> names.add(n.get("fixture").asText()));
    assertEquals(all, names);
  }
}
