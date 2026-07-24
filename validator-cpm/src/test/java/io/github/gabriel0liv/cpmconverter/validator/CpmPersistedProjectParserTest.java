package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CpmPersistedProjectParserTest {
  @Test
  void malformedRootProducesFailureInsteadOfThrowing() throws Exception {
    var json = new ObjectMapper().readTree("{\"version\":1,\"elements\":[null]}");
    var config = new CpmValidatedConfigV1(json, 1, "default", new CpmPersistedSize2i(64, 64), false, false);
    var result = assertDoesNotThrow(() -> new CpmPersistedProjectParser().parse(json, config, null, false, CpmArtifactLimits.defaults()));
    assertFalse(result.success());
  }
}
