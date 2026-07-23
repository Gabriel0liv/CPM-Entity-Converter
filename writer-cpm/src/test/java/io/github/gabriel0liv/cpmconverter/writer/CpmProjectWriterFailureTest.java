package io.github.gabriel0liv.cpmconverter.writer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CpmProjectWriterFailureTest {
  @Test
  void expectedInvalidRequestsReturnDiagnosticsInsteadOfThrowing() {
    var writer = new CpmProjectWriter();
    for (var request : new CpmProjectWriteRequest[] {null, new CpmProjectWriteRequest(null, null), new CpmProjectWriteRequest(null, new byte[0])}) {
      var result = writer.write(request);
      assertFalse(result.success());
      assertTrue(result.diagnostics().hasErrors());
      assertEquals("CPM_WRITE_FAILED", result.diagnostics().errors().get(0).code().value());
    }
  }
}
