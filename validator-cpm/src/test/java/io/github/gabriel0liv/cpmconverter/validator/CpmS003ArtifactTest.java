package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class CpmS003ArtifactTest {
  @Test void validatesIndependentS003Corpus() throws Exception {
    Path script = Path.of("..", "spikes", "minimal-cpmproject", "scripts", "generate_and_verify.py");
    if (!Files.exists(script)) script = Path.of("spikes", "minimal-cpmproject", "scripts", "generate_and_verify.py");
    var process = new ProcessBuilder("python", script.toString()).redirectErrorStream(true).start();
    assertEquals(0, process.waitFor(), "S003 generator failed: " + new String(process.getInputStream().readAllBytes()));
    Path root=Path.of("..","spikes","minimal-cpmproject","artifacts");
    var validator=new CpmArtifactValidator();
    for(int i=0;i<=5;i++) {
      var result=validator.validate(Files.readAllBytes(root.resolve("M"+i+".cpmproject")));
      if(i<2) assertFalse(result.success(), "M"+i);
      else {
        assertTrue(result.success(), "M"+i+" "+result.diagnostics().all());
        if (i == 3) assertTrue(result.value().project().persistedTargets().containsKey(1000L));
        if (i == 5) {
          assertEquals(1, result.value().animations().size());
          assertTrue(result.value().summary().frameCount() > 0);
          assertTrue(result.value().summary().componentReferenceCount() > 0);
          assertTrue(result.value().project().effectiveTargets().containsKey(1000L));
        }
      }
    }
  }
}
