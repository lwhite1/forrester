package systems.courant.sd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmoothTest {

    @Test
    public void shouldReturnInitialValueAtStepZero() {
        int[] step = {0};
        Smooth formula = Smooth.of(() -> 100, 5, 50, () -> step[0]);
        assertEquals(50, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldDefaultInitialToFirstInput() {
        int[] step = {0};
        Smooth formula = Smooth.of(() -> 100, 5, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldApproachInputOverTime() {
        int[] step = {0};
        Smooth formula = Smooth.of(() -> 100, 5, 0, () -> step[0]);

        // Initialize at step 0
        assertEquals(0, formula.getCurrentValue(), 0.01);

        // Advance steps — should approach 100
        double prev = 0;
        for (int i = 1; i <= 20; i++) {
            step[0] = i;
            double val = formula.getCurrentValue();
            assertTrue(val > prev, "Should be increasing toward input");
            prev = val;
        }
        // After 20 steps with smoothingTime=5, should be very close to 100
        assertTrue(prev > 95, "Should have converged near input after 20 steps");
    }

    @Test
    public void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Smooth formula = Smooth.of(() -> 100, 5, 50, () -> step[0]);

        double first = formula.getCurrentValue();
        double second = formula.getCurrentValue();
        assertEquals(first, second, 0.0);
    }

    @Test
    public void shouldRemainStableWhenInputEqualsSmoothed() {
        int[] step = {0};
        Smooth formula = Smooth.of(() -> 50, 5, 50, () -> step[0]);

        formula.getCurrentValue(); // initialize
        step[0] = 1;
        assertEquals(50, formula.getCurrentValue(), 0.01);
        step[0] = 10;
        assertEquals(50, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldReadInputOncePerEvaluationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Smooth formula = Smooth.of(() -> { readCount[0]++; return 100; }, 5, 0, () -> step[0]);

        formula.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should be read exactly once (not once per sub-step)
        step[0] = 5;
        formula.getCurrentValue();
        assertEquals(1, readCount[0],
                "Input should be read once per evaluation, not once per sub-step");
    }

    @Test
    public void shouldRejectNonPositiveSmoothingTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Smooth.of(() -> 100, 0, () -> 0));
        assertThrows(IllegalArgumentException.class, () ->
                Smooth.of(() -> 100, -1, () -> 0));
    }

    @Test
    public void shouldClampDynamicSmoothingTimeWhenZero() {
        int[] step = {0};
        double[] smoothTime = {5};
        Smooth formula = Smooth.of(() -> 100, () -> smoothTime[0], 0, () -> step[0]);

        formula.getCurrentValue(); // initialize

        step[0] = 1;
        double afterNormal = formula.getCurrentValue();
        assertTrue(afterNormal > 0, "Should advance toward input");

        // Set smoothingTime to 0 — should clamp to 1.0, not produce NaN
        smoothTime[0] = 0;
        step[0] = 2;
        double afterZero = formula.getCurrentValue();
        assertFalse(Double.isNaN(afterZero), "Should not produce NaN when smoothingTime is zero");
        assertFalse(Double.isInfinite(afterZero), "Should not produce Infinity when smoothingTime is zero");
        assertTrue(afterZero > afterNormal, "Should still advance toward input");
    }

    @Test
    public void shouldClampDynamicSmoothingTimeWhenNegative() {
        int[] step = {0};
        double[] smoothTime = {5};
        Smooth formula = Smooth.of(() -> 100, () -> smoothTime[0], 0, () -> step[0]);

        formula.getCurrentValue(); // initialize

        step[0] = 1;
        formula.getCurrentValue();

        smoothTime[0] = -3;
        step[0] = 2;
        double afterNegative = formula.getCurrentValue();
        assertFalse(Double.isNaN(afterNegative), "Should not produce NaN when smoothingTime is negative");
        assertFalse(Double.isInfinite(afterNegative), "Should not produce Infinity when smoothingTime is negative");
    }

    @Test
    public void shouldResetNonPositiveWarningFlag() {
        int[] step = {0};
        double[] smoothTime = {0};
        Smooth formula = Smooth.of(() -> 100, () -> smoothTime[0], 0, () -> step[0]);

        formula.getCurrentValue(); // initialize
        step[0] = 1;
        double val1 = formula.getCurrentValue(); // triggers clamp + warning
        assertFalse(Double.isNaN(val1));

        formula.reset();
        step[0] = 0;
        formula.getCurrentValue(); // re-initialize
        step[0] = 1;
        double val2 = formula.getCurrentValue(); // should clamp again, no crash
        assertFalse(Double.isNaN(val2));
    }

    @Test
    public void shouldAcceptVariableSmoothingTime() {
        int[] step = {0};
        double[] smoothTime = {5};
        Smooth formula = Smooth.of(() -> 100, () -> smoothTime[0], 0, () -> step[0]);

        // Initialize at step 0
        assertEquals(0, formula.getCurrentValue(), 0.01);

        // Advance with smoothingTime=5: adjustment = (100-0)/5 = 20
        step[0] = 1;
        assertEquals(20, formula.getCurrentValue(), 0.01);

        // Change smoothingTime to 2 for faster convergence: adjustment = (100-20)/2 = 40
        smoothTime[0] = 2;
        step[0] = 2;
        assertEquals(60, formula.getCurrentValue(), 0.01);
    }
}
