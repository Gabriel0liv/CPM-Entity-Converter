package io.github.gabriel0liv.cpmconverter.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DiagnosticCatalogTest {
  @Test
  void catalogHasNoDuplicatesAndIsDocumented() throws Exception {
    HashSet<String> values = new HashSet<>();
    for (Field field : DiagnosticCodes.class.getFields()) {
      assertTrue(values.add((String) field.get(null)), "duplicate " + field.getName());
    }
    String documentation =
        Files.readString(Path.of("..", "specs", "001-geckolib4-to-cpm", "diagnostics.md"));
    assertEquals(values.size(), DiagnosticCodes.all().size());
    String block =
        documentation.substring(
            documentation.indexOf("<!-- NORMATIVE-CATALOG-BEGIN -->"),
            documentation.indexOf("<!-- NORMATIVE-CATALOG-END -->"));
    HashSet<String> documented = new HashSet<>();
    var matcher = Pattern.compile("`([A-Z][A-Z0-9_]+)`").matcher(block);
    while (matcher.find()) documented.add(matcher.group(1));
    assertEquals(values, documented, "catalog and diagnostics.md differ");
    assertEquals(values.size(), matcherResults(block), "duplicate code in normative block");
  }

  private static int matcherResults(String block) {
    return (int) Pattern.compile("`([A-Z][A-Z0-9_]+)`").matcher(block).results().count();
  }
}
