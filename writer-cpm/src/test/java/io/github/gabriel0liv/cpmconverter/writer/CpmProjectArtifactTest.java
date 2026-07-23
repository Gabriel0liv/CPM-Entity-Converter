package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectArtifactTest {
  @Test
  void artifactDefensivelyCopiesBytesAndComparesContent() {
    byte[] input = {1, 2, 3};
    var artifact = CpmProjectArtifact.of(input);
    input[0] = 9;
    var returned = artifact.bytes();
    returned[1] = 8;
    assertArrayEquals(new byte[] {1, 2, 3}, artifact.bytes());
    assertEquals(3, artifact.size());
    assertEquals(artifact, CpmProjectArtifact.of(new byte[] {1, 2, 3}));
    assertEquals(artifact.hashCode(), CpmProjectArtifact.of(new byte[] {1, 2, 3}).hashCode());
    assertThrows(IllegalArgumentException.class, () -> CpmProjectArtifact.of(new byte[0]));
    assertFalse(artifact.toString().contains("1, 2, 3"));
  }

  @Test
  void writeRequestDefensivelyCopiesSkinBytes() {
    byte[] png = {8, 9};
    var request = new CpmProjectWriteRequest(null, png);
    png[0] = 0;
    assertArrayEquals(new byte[] {8, 9}, request.skinPng());
    var returned = request.skinPng();
    returned[1] = 0;
    assertArrayEquals(new byte[] {8, 9}, request.skinPng());
  }
}
