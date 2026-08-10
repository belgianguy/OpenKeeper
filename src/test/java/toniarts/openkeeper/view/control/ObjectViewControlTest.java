package toniarts.openkeeper.view.control;

import com.jme3.math.FastMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectViewControlTest {

    @Test
    void convertsDkiiAngularIncrementUsingLevelTickRate() {
        assertEquals(FastMath.TWO_PI, ObjectViewControl.toRadiansPerSecond(512, 4), 0.00001f);
        assertEquals(-FastMath.TWO_PI, ObjectViewControl.toRadiansPerSecond(-512, 4), 0.00001f);
    }
}
