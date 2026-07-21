package io.github.gabriel0liv.cpmconverter;
import org.junit.jupiter.api.*; import static org.junit.jupiter.api.Assertions.*; import io.github.gabriel0liv.cpmconverter.math.*; import io.github.gabriel0liv.cpmconverter.diagnostics.*; import java.util.*;
class CoreTest {
 @Test void vectorsAndQuaternion(){assertEquals(new Vec3d(1,1,1),new Vec3d(1,2,3).add(new Vec3d(0,-1,-2))); assertEquals(0,new Vec3d(1,0,0).dot(new Vec3d(0,1,0))); assertEquals(new Vec3d(0,0,1),new Vec3d(1,0,0).cross(new Vec3d(0,1,0)));var r=Quatd.fromEulerZYX(0,0,Math.PI/2).rotate(new Vec3d(1,0,0));assertEquals(0,r.x(),1e-9);assertEquals(1,r.y(),1e-9);assertEquals(0,r.z(),1e-9);}
 @Test void diagnosticsDeterministic(){var b=new DiagnosticBag().add(Diagnostic.of(Severity.WARNING,"W","w")).add(Diagnostic.of(Severity.ERROR,"E","e"));assertTrue(b.hasErrors());assertEquals(List.of(Severity.ERROR,Severity.WARNING),b.all().stream().map(Diagnostic::severity).toList());assertThrows(IllegalArgumentException.class,()->new SourcePath("C:\\absolute.json"));}
 @Test void coordinateBoundary(){assertEquals(new Vec3d(-1,-2,3),CoordinateBoundary.geckoToCpmPosition(new Vec3d(1,2,3)));}
}
