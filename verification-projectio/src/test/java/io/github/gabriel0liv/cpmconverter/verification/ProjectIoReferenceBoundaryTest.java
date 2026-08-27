package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProjectIoReferenceBoundaryTest {
  @Test
  void pinsExactCpmVersionAndCommit() {
    assertEquals("0.6.27", ProjectIoReference.CPM_VERSION);
    assertEquals("9272f4f9c36a2bbd6986e6da65bf7091369cb12b", ProjectIoReference.CPM_COMMIT);
  }
}
