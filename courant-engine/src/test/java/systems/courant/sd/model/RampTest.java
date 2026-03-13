package systems.courant.sd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RampTest {

    @Test
    public void shouldReturnZeroBeforeStartStep() {
        int[] step = {2};
        Ramp formula = Ramp.of(10, 5, () -> step[0]);
        assertEquals(0, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldReturnZeroAtStartStep() {
        int[] step = {5};
        Ramp formula = Ramp.of(10, 5, () -> step[0]);
        assertEquals(0, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldIncreaseLinearlyAfterStart() {
        int[] step = {8};
        Ramp formula = Ramp.of(10, 5, () -> step[0]);
        // 8 - 5 = 3 elapsed, 3 * 10 = 30
        assertEquals(30, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldHoldConstantAfterEndStep() {
        int[] step = {20};
        Ramp formula = Ramp.of(10, 5, 15, () -> step[0]);
        // capped at (15 - 5) * 10 = 100
        assertEquals(100, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldReturnCorrectValueAtEndStep() {
        int[] step = {15};
        Ramp formula = Ramp.of(10, 5, 15, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldSupportNegativeSlope() {
        int[] step = {7};
        Ramp formula = Ramp.of(-5, 5, () -> step[0]);
        assertEquals(-10, formula.getCurrentValue(), 0.0);
    }

    @Test
    public void shouldRejectEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () ->
                Ramp.of(10, 10, 5, () -> 0));
    }
}
