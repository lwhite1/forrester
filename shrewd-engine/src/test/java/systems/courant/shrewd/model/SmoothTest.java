package systems.courant.shrewd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void shouldReadInputEachIterationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Smooth formula = Smooth.of(() -> { readCount[0]++; return 100; }, 5, 0, () -> step[0]);

        formula.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should be read once per catch-up iteration
        step[0] = 5;
        formula.getCurrentValue();
        assertEquals(5, readCount[0],
                "Input should be read once per catch-up iteration");
    }

    @Test
    public void shouldRejectNonPositiveSmoothingTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Smooth.of(() -> 100, 0, () -> 0));
        assertThrows(IllegalArgumentException.class, () ->
                Smooth.of(() -> 100, -1, () -> 0));
    }
}
