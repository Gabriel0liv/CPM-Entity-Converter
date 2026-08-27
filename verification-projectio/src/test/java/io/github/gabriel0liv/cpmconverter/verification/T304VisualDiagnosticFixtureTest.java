package io.github.gabriel0liv.cpmconverter.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gabriel0liv.cpmconverter.cpm.CpmElementV1;
import io.github.gabriel0liv.cpmconverter.cpm.CpmRootV1;
import io.github.gabriel0liv.cpmconverter.ir.FaceUvIR;
import io.github.gabriel0liv.cpmconverter.ir.PerFaceUvIR;
import io.github.gabriel0liv.cpmconverter.math.Vec3d;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class T304VisualDiagnosticFixtureTest {
  private final CurrentFixturePipeline pipeline = new CurrentFixturePipeline();

  @Test
  void fixtureAHasReadableHumanoidProportions() throws Exception {
    CurrentFixtureArtifact artifact = pipeline.generate("fixture-a-humanoid");

    assertEquals(new Vec3d(0, -24, 0), element(artifact, "body").transform().translation());
    assertEquals(new Vec3d(8, 12, 4), element(artifact, "body#cube-0").size());
    assertEquals(new Vec3d(8, 8, 8), element(artifact, "head#cube-0").size());
    assertEquals(new Vec3d(4, 12, 4), element(artifact, "left_arm#cube-0").size());
    assertEquals(new Vec3d(4, 12, 4), element(artifact, "right_arm#cube-0").size());
    assertEquals(new Vec3d(4, 12, 4), element(artifact, "left_leg#cube-0").size());
    assertEquals(new Vec3d(4, 12, 4), element(artifact, "right_leg#cube-0").size());
  }

  @Test
  void fixtureBHasReadableNeckHeadHornChain() throws Exception {
    CurrentFixtureArtifact artifact = pipeline.generate("fixture-b-neck");

    assertEquals(new Vec3d(0, -24, 0), element(artifact, "body").transform().translation());
    assertEquals(new Vec3d(8, 12, 4), element(artifact, "body#cube-0").size());
    assertEquals(new Vec3d(4, 4, 4), element(artifact, "neck#cube-0").size());
    assertEquals(new Vec3d(8, 8, 8), element(artifact, "head#cube-0").size());
    assertEquals(new Vec3d(2, 6, 2), element(artifact, "horn#cube-0").size());
  }

  @Test
  void fixtureCHasDistinctPerFaceUvRegionsAndColors() throws Exception {
    CurrentFixtureArtifact artifact = pipeline.generate("fixture-c-deep-hierarchy");
    CpmElementV1 accessory = element(artifact, "accessory#cube-0");
    PerFaceUvIR perFace = assertInstanceOf(PerFaceUvIR.class, accessory.uv());

    assertEquals(6, perFace.faces().size());
    Set<String> regions = new HashSet<>();
    for (FaceUvIR face : perFace.faces().values()) {
      regions.add(face.u() + ":" + face.v() + ":" + face.width() + ":" + face.height());
    }
    assertEquals(6, regions.size(), "each accessory face must use a distinct UV rectangle");

    BufferedImage texture =
        ImageIO.read(new ByteArrayInputStream(artifact.project().textures().get(0).pngBytes()));
    Set<Integer> colors = new HashSet<>();
    for (FaceUvIR face : perFace.faces().values()) {
      int x = (int) Math.floor(face.u() + Math.abs(face.width()) / 2.0);
      int y = (int) Math.floor(face.v() + Math.abs(face.height()) / 2.0);
      assertTrue(x >= 0 && x < texture.getWidth());
      assertTrue(y >= 0 && y < texture.getHeight());
      colors.add(texture.getRGB(x, y));
    }
    assertEquals(6, colors.size(), "the six face regions must be visually distinguishable");
  }

  @Test
  void fixtureDHasReadableQuadrupedProportions() throws Exception {
    CurrentFixtureArtifact artifact = pipeline.generate("fixture-d-quadruped");

    assertEquals(new Vec3d(0, -16, 0), element(artifact, "body").transform().translation());
    assertEquals(new Vec3d(10, 6, 16), element(artifact, "body#cube-0").size());
    assertEquals(new Vec3d(6, 6, 6), element(artifact, "head#cube-0").size());
    assertEquals(new Vec3d(2, 8, 2), element(artifact, "leg_fl#cube-0").size());
    assertEquals(new Vec3d(2, 8, 2), element(artifact, "leg_fr#cube-0").size());
    assertEquals(new Vec3d(2, 8, 2), element(artifact, "leg_bl#cube-0").size());
    assertEquals(new Vec3d(2, 8, 2), element(artifact, "leg_br#cube-0").size());
    assertEquals(new Vec3d(2, 2, 8), element(artifact, "tail#cube-0").size());
  }

  private static CpmElementV1 element(CurrentFixtureArtifact artifact, String name) {
    for (CpmRootV1 root : artifact.project().roots()) {
      for (CpmElementV1 child : root.children()) {
        CpmElementV1 match = find(child, name);
        if (match != null) return match;
      }
    }
    throw new AssertionError("missing element " + name + " in " + artifact.fixture());
  }

  private static CpmElementV1 find(CpmElementV1 element, String name) {
    if (element.name().equals(name)) return element;
    for (CpmElementV1 child : element.children()) {
      CpmElementV1 match = find(child, name);
      if (match != null) return match;
    }
    return null;
  }
}
