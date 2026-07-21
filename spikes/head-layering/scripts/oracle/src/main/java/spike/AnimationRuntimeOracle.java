// NON_PRODUCTION: executable observation of CPM Animation + RenderedCube behavior.
package spike;

import java.util.Locale;

import com.tom.cpl.math.Vec3f;
import com.tom.cpm.shared.animation.Animation;
import com.tom.cpm.shared.animation.IModelComponent;
import com.tom.cpm.shared.animation.interpolator.InterpolatorType;
import com.tom.cpm.shared.model.Cube;
import com.tom.cpm.shared.model.RenderedCube;

public final class AnimationRuntimeOracle {
    private static RenderedCube cube() {
        Cube source = new Cube();
        source.offset = new Vec3f();
        source.pos = new Vec3f(1, 2, 3);
        source.rotation = new Vec3f(0, (float) Math.toRadians(5), 0);
        source.size = new Vec3f(1, 1, 1);
        source.scale = new Vec3f(1, 1, 1);
        source.meshScale = new Vec3f(1, 1, 1);
        source.rgb = 0xffffff;
        return new RenderedCube(source);
    }

    private static Animation animation(RenderedCube cube, int duration, int priority,
            boolean additive, InterpolatorType type, float[] posX, float[] rotY,
            float[] scaleX, float[] scaleY, float[] scaleZ) {
        int frames = posX.length;
        float[][][] data = new float[1][12][frames];
        Boolean[][] show = new Boolean[1][frames];
        for (int frame = 0; frame < frames; frame++) {
            data[0][0][frame] = posX[frame];
            data[0][4][frame] = rotY[frame];
            data[0][6][frame] = 255;
            data[0][7][frame] = 255;
            data[0][8][frame] = 255;
            data[0][9][frame] = scaleX[frame];
            data[0][10][frame] = scaleY[frame];
            data[0][11][frame] = scaleZ[frame];
            show[0][frame] = true;
        }
        return new Animation(new IModelComponent[] {cube}, data, show, duration, priority,
                additive, type);
    }

    private static float[] repeat(float value, int size) {
        float[] values = new float[size];
        java.util.Arrays.fill(values, value);
        return values;
    }

    private static void timeline(String name, InterpolatorType type, int frames) {
        RenderedCube cube = cube();
        float[] position = new float[frames];
        for (int i = 0; i < frames; i++) position[i] = i * 10;
        Animation animation = animation(cube, 1000, 0, false, type, position,
                repeat(0, frames), repeat(1, frames), repeat(1, frames), repeat(1, frames));
        long[] times = {0, 1, 499, 500, 999, 1000, 1001, 1500};
        StringBuilder values = new StringBuilder();
        for (long time : times) {
            cube.reset();
            animation.animate(null, time, null, null);
            if (values.length() > 0) values.append(',');
            values.append(String.format(Locale.ROOT, "{\"t\":%d,\"x\":%.6f}", time, cube.pos.x));
        }
        System.out.printf("{\"marker\":\"NON_PRODUCTION\",\"test\":\"%s\",\"frames\":%d,\"values\":[%s]}%n",
                name, frames, values);
    }

    private static void layering() {
        RenderedCube cube = cube();
        float rad10 = (float) Math.toRadians(10);
        float rad20 = (float) Math.toRadians(20);
        Animation absoluteBase = animation(cube, 1000, 0, false, InterpolatorType.LINEAR_LOOP,
                repeat(0, 2), repeat(rad10, 2), repeat(1, 2), repeat(1, 2), repeat(1, 2));
        Animation additiveBase = animation(cube, 1000, 0, true, InterpolatorType.LINEAR_LOOP,
                repeat(0, 2), repeat(rad10, 2), repeat(1, 2), repeat(1, 2), repeat(1, 2));
        Animation additiveLook = animation(cube, 1001, 1, true, InterpolatorType.LINEAR_SINGLE,
                repeat(0, 2), repeat(rad20, 2), repeat(0, 2), repeat(2, 2), repeat(0, 2));

        cube.reset(); absoluteBase.animate(null, 500, null, null); additiveLook.animate(null, 500, null, null);
        double absoluteThenLook = Math.toDegrees(cube.rotation.y);
        double scaleX = cube.renderScale.x, scaleY = cube.renderScale.y, scaleZ = cube.renderScale.z;
        cube.reset(); additiveLook.animate(null, 500, null, null); absoluteBase.animate(null, 500, null, null);
        double lookThenAbsolute = Math.toDegrees(cube.rotation.y);
        cube.reset(); additiveBase.animate(null, 500, null, null); additiveLook.animate(null, 500, null, null);
        double bothAdditive = Math.toDegrees(cube.rotation.y);
        for (int i = 0; i < 100; i++) {
            cube.reset(); absoluteBase.animate(null, 999, null, null); additiveLook.animate(null, 500, null, null);
        }
        double after100Resets = Math.toDegrees(cube.rotation.y);
        System.out.printf(Locale.ROOT,
                "{\"marker\":\"NON_PRODUCTION\",\"test\":\"layering\",\"absoluteThenLookDeg\":%.6f,\"lookThenAbsoluteDeg\":%.6f,\"bothAdditiveDeg\":%.6f,\"after100ResetLoopsDeg\":%.6f,\"scaleAfterZeroTwoZero\":[%.6f,%.6f,%.6f]}%n",
                absoluteThenLook, lookThenAbsolute, bothAdditive, after100Resets, scaleX, scaleY, scaleZ);
    }

    public static void main(String[] args) {
        timeline("loop-one-frame", InterpolatorType.LINEAR_LOOP, 1);
        timeline("loop-two-frames", InterpolatorType.LINEAR_LOOP, 2);
        timeline("loop-three-frames", InterpolatorType.LINEAR_LOOP, 3);
        timeline("single-one-frame", InterpolatorType.LINEAR_SINGLE, 1);
        timeline("single-two-frames", InterpolatorType.LINEAR_SINGLE, 2);
        timeline("single-three-frames", InterpolatorType.LINEAR_SINGLE, 3);
        layering();
    }
}
