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
    for (String value : values) {
      assertTrue(documentation.contains("`" + value + "`") || documentation.contains(value), value);
    }
    HashSet<String> documented = new HashSet<>();
    var matcher = Pattern.compile("`([A-Z][A-Z0-9_]+)`").matcher(documentation);
    while (matcher.find()) documented.add(matcher.group(1));
    documented.remove("ERROR");
    documented.remove("WARNING");
    documented.remove("INFO");
    assertEquals(values, documented, "catalog and diagnostics.md differ");
  }
}
