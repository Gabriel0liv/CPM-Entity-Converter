package io.github.gabriel0liv.cpmconverter.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingValidatorTest {
  @Test
  void overrotationRequiresExplicitPermission() {
    var look =
        new MappingDocumentV1.Look("head", "neck", "inherited_split", 0.8, 0.8, false, Map.of());
    var document = new MappingDocumentV1(1, Map.of(), Map.of(), look, null, List.of());
    assertTrue(new MappingValidator().validate(document).hasErrors());
  }

  @Test
  void permittedOverrotationIsAccepted() {
    var look =
        new MappingDocumentV1.Look("head", "neck", "inherited_split", 0.8, 0.8, true, Map.of());
    var document = new MappingDocumentV1(1, Map.of(), Map.of(), look, null, List.of());
    assertFalse(new MappingValidator().validate(document).hasErrors());
  }
}
