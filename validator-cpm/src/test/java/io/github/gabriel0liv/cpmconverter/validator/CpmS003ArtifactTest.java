package io.github.gabriel0liv.cpmconverter.validator;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class CpmS003ArtifactTest {
  @Test void validatesIndependentS003Corpus() throws Exception {
    Path root=Path.of("..","spikes","minimal-cpmproject","artifacts");
    var validator=new CpmArtifactValidator();
    for(int i=0;i<=5;i++) { var result=validator.validate(Files.readAllBytes(root.resolve("M"+i+".cpmproject"))); if(i<2) assertFalse(result.success(), "M"+i); else assertTrue(result.success(), "M"+i+" "+result.diagnostics().all()); }
  }
}
