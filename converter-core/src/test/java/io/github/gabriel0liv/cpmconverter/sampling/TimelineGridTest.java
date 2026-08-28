package io.github.gabriel0liv.cpmconverter.sampling;

import static org.junit.jupiter.api.Assertions.*;

import io.github.gabriel0liv.cpmconverter.ir.ClipId;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineGridTest {
  @Test
  void loopAt20FpsDoesNotDuplicateDurationEndpoint() {
    var grid = TimelineGrid.build(1.0, 20, TimelineKind.LOOP);

    assertEquals(20, grid.metadata().frameCount());
    assertEquals(0.0, grid.times().get(0), 0.0);
    assertEquals(0.95, grid.times().get(19), 1e-12);
    assertFalse(grid.times().contains(1.0));
    assertEquals(0.05, grid.metadata().frameInterval(), 1e-12);
    assertEquals(20.0, grid.metadata().frameDensity(), 1e-12);
    assertEquals(20.0, grid.metadata().effectiveIntervalRate(), 1e-12);
  }

  @Test
  void singleAt20FpsIncludesExactDurationEndpoint() {
    var grid = TimelineGrid.build(1.0, 20, TimelineKind.SINGLE);

    assertEquals(20, grid.metadata().frameCount());
    assertEquals(0.0, grid.times().get(0), 0.0);
    assertEquals(1.0, grid.times().get(19), 0.0);
    assertEquals(1.0 / 19.0, grid.metadata().frameInterval(), 1e-12);
    assertEquals(19.0, grid.metadata().effectiveIntervalRate(), 1e-12);
    assertEquals(20.0, grid.metadata().frameDensity(), 1e-12);
  }

  @Test
  void nOneSingleUsesZeroIntervalAndRate() {
    var grid = TimelineGrid.build(0.01, 20, TimelineKind.SINGLE);

    assertEquals(List.of(0.0), grid.times());
    assertEquals(1, grid.metadata().frameCount());
    assertEquals(0.0, grid.metadata().frameInterval(), 0.0);
    assertEquals(0.0, grid.metadata().effectiveIntervalRate(), 0.0);
    assertEquals(100.0, grid.metadata().frameDensity(), 1e-12);
  }

  @Test
  void frameCountUsesJavaMathRoundAtHalfBoundary() {
    assertEquals(3, TimelineGrid.build(0.125, 20, TimelineKind.LOOP).metadata().frameCount());
  }

  @Test
  void timestampsAreIndexDerivedAndMetadataRecordsGridError() {
    var grid = TimelineGrid.build(0.37, 20, TimelineKind.LOOP);

    for (int i = 0; i < grid.times().size(); i++) {
      assertEquals(i * 0.37 / grid.metadata().frameCount(), grid.times().get(i), 0.0);
    }
    double expected = 0;
    for (int i = 0; i < grid.times().size(); i++) {
      expected = Math.max(expected, Math.abs(grid.times().get(i) - i / 20.0));
    }
    assertEquals(expected, grid.metadata().maxTemporalGridError(), 0.0);
  }

  @Test
  void requestAndKeyRejectFpsOutsideOneToTwoForty() {
    var clip = new ClipId("walk");

    assertThrows(IllegalArgumentException.class, () -> new SamplingRequest(clip, 0, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> new SamplingRequest(clip, 241, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> new SampledClipKey(clip, 0, TimelineKind.SINGLE));
    assertThrows(IllegalArgumentException.class, () -> new SampledClipKey(clip, 241, TimelineKind.SINGLE));
  }

  @Test
  void requestAndKeyPreserveIdentityFields() {
    var clip = new ClipId("walk");
    var request = new SamplingRequest(clip, 20, TimelineKind.LOOP);
    var key = new SampledClipKey(clip, 20, TimelineKind.LOOP);

    assertEquals(clip, request.clipId());
    assertEquals(20, request.requestedFps());
    assertEquals(TimelineKind.LOOP, request.timelineKind());
    assertEquals(new SampledClipKey(clip, 20, TimelineKind.LOOP), key);
  }

  @Test
  void timelineGridRejectsInvalidDurationAndFps() {
    assertThrows(IllegalArgumentException.class, () -> TimelineGrid.build(0.0, 20, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> TimelineGrid.build(Double.NaN, 20, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> TimelineGrid.build(1.0, 0, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> TimelineGrid.build(1.0, 241, TimelineKind.LOOP));
    assertThrows(IllegalArgumentException.class, () -> TimelineGrid.build(1.0, 20, null));
  }
}
