package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CpmDeterministicZipWriterTest {
  @Test
  void entriesOrderMetadataAndBytesAreDeterministic() throws Exception {
    byte[] config = "{\"version\":1}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] skin = {0x55, 0x54, 0x05, 0x00, 0x01, 3};
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
      TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon")); var lisbon = writer.write(config, skin);
      TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles")); var la = writer.write(config, skin);
      assertArrayEquals(utc, lisbon);
      assertArrayEquals(utc, la);
    } finally { TimeZone.setDefault(previous); }
  }

}
