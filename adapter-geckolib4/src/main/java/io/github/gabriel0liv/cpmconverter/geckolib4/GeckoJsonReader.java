package io.github.gabriel0liv.cpmconverter.geckolib4;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Bounded JSON reader shared by the Gecko geometry and animation adapters. */
final class GeckoJsonReader {
  private final GeckoInputLimits limits;
  private final ObjectMapper mapper;

  GeckoJsonReader(GeckoInputLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
    JsonFactory factory =
        JsonFactory.builder()
            .streamReadConstraints(
                StreamReadConstraints.builder()
                    .maxDocumentLength(limits.maxJsonBytes())
                    .maxNestingDepth(limits.maxJsonDepth())
                    .build())
            .build();
    this.mapper = new ObjectMapper(factory);
  }

  JsonNode read(Path path) throws IOException, InputLimitException {
    long size = Files.size(path);
    if (size > limits.maxJsonBytes()) {
      throw new InputLimitException(
          "input size " + size + " bytes exceeds limit " + limits.maxJsonBytes());
    }

    try (InputStream input = Files.newInputStream(path)) {
      return mapper.readTree(input);
    } catch (StreamConstraintsException exception) {
      throw new InputLimitException(exception.getMessage());
    }
  }

  static final class InputLimitException extends Exception {
    private static final long serialVersionUID = 1L;

    InputLimitException(String message) {
      super(message);
    }
  }
}
