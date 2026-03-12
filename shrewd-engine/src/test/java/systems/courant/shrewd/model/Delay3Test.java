package systems.courant.shrewd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Delay3Test {

    @Test
    public void shouldReturnInitialValueAtStepZero() {
        int[] step = {0};
        Delay3 formula = Delay3.of(() -> 100, 6, 50, () -> step[0]);
        assertEquals(50, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldDefaultInitialToFirstInput() {
        int[] step = {0};
        Delay3 formula = Delay3.of(() -> 100, 6, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldRemainStableWhenInputEqualsInitial() {
        int[] step = {0};
        Delay3 formula = Delay3.of(() -> 50, 6, 50, () -> step[0]);

        formula.getCurrentValue(); // initialize
        for (int i = 1; i <= 10; i++) {
            step[0] = i;
            assertEquals(50, formula.getCurrentValue(), 0.01,
                    "Output should remain stable at step " + i);
        }
    }

    @Test
    public void shouldEventuallyConvergeToNewInput() {
        int[] step = {0};
        double[] input = {50};
        Delay3 formula = Delay3.of(() -> input[0], 6, 50, () -> step[0]);

        formula.getCurrentValue(); // initialize

        // Step input to 100
        input[0] = 100;

        double prev = 50;
        for (int i = 1; i <= 50; i++) {
            step[0] = i;
            double val = formula.getCurrentValue();
            assertTrue(val >= prev - 0.01,
                    "Output should be non-decreasing toward new input at step " + i);
            prev = val;
        }
        // After many steps, should converge near 100
        assertTrue(prev > 95, "Should have converged near 100 after 50 steps");
    }

    @Test
    public void shouldProduceDelayedResponse() {
        int[] step = {0};
        double[] input = {0};
        Delay3 formula = Delay3.of(() -> input[0], 6, 0, () -> step[0]);

        formula.getCurrentValue(); // initialize

        // Apply a step input of 100
        input[0] = 100;

        step[0] = 1;
        double early = formula.getCurrentValue();

        // Advance further
        for (int i = 2; i <= 12; i++) {
            step[0] = i;
            formula.getCurrentValue();
        }
        double late = formula.getCurrentValue();

        // Early output should be much less than late output (delay effect)
        assertTrue(early < late, "Early output should be less than late output due to delay");
        assertTrue(early < 50, "Output at step 1 should be well below the input of 100");
    }

    @Test
    public void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Delay3 formula = Delay3.of(() -> 100, 6, 50, () -> step[0]);

        double first = formula.getCurrentValue();
        double second = formula.getCurrentValue();
        assertEquals(first, second, 0.0);
    }

    @Test
    public void shouldReadInputOncePerEvaluationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Delay3 formula = Delay3.of(() -> { readCount[0]++; return 100; }, 6, 100, () -> step[0]);

        formula.getCurrentValue(); // initialize
        readCount[0] = 0;

        // Jump by 5 steps — input should be read exactly once (not once per sub-step)
        step[0] = 5;
        formula.getCurrentValue();
        assertEquals(1, readCount[0],
                "Input should be read once per evaluation, not once per sub-step");
    }

    @Test
    public void shouldRejectNonPositiveDelayTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Delay3.of(() -> 100, 0, () -> 0));
        assertThrows(IllegalArgumentException.class, () ->
                Delay3.of(() -> 100, -1, () -> 0));
    }
}
