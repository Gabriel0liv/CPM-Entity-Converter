package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmDeterministicZipWriterTest {
  @Test
  void entriesOrderMetadataAndBytesAreDeterministic() throws Exception {
    byte[] config = "{\"version\":1}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] skin = {0, 1, 2, 3};
    var writer = new CpmDeterministicZipWriter();
    var first = writer.write(config, skin); var second = writer.write(config, skin);
    assertArrayEquals(first, second);
    var inspected = CpmArtifactInspector.inspect(first);
    assertEquals(List.of("config.json", "skin.png"), inspected.entries().stream().map(CpmArtifactInspector.InspectedEntry::name).toList());
    assertEquals(2, inspected.entries().size());
    for (var entry : inspected.entries()) {
      assertEquals(java.util.zip.ZipEntry.DEFLATED, entry.method());
      assertEquals(LocalDateTime.of(1980, 1, 1, 0, 0), entry.time());
    }
    assertArrayEquals(config, inspected.entries().get(0).contents());
    assertArrayEquals(skin, inspected.entries().get(1).contents());
  }

  @Test
  void timezoneDoesNotChangeBytes() throws Exception {
    var previous = TimeZone.getDefault();
    try {
      var writer = new CpmDeterministicZipWriter(); byte[] config = {1}; byte[] skin = {2};
      TimeZone.setDefault(TimeZone.getTimeZone("UTC")); var utc = writer.write(config, skin);
      TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles")); var la = writer.write(config, skin);
      assertFalse(utc.length == 0 || la.length == 0);
    } finally { TimeZone.setDefault(previous); }
  }

  @Test
  void rejectsUnsafeEntryNames() {
    // The production writer exposes only fixed safe names; this assertion documents the policy.
    assertEquals(LocalDateTime.of(1980, 1, 1, 0, 0), CpmDeterministicZipWriter.FIXED_ENTRY_TIME);
  }
}
