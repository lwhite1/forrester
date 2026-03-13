package systems.courant.sd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Delay1Test {

    @Test
    public void shouldReturnInitialValueAtStepZero() {
        int[] step = {0};
        Delay1 formula = Delay1.of(() -> 100, 6, 50, () -> step[0]);
        assertEquals(50, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldDefaultInitialToFirstInput() {
        int[] step = {0};
        Delay1 formula = Delay1.of(() -> 100, 6, () -> step[0]);
        assertEquals(100, formula.getCurrentValue(), 0.01);
    }

    @Test
    public void shouldRemainStableWhenInputEqualsInitial() {
        int[] step = {0};
        Delay1 formula = Delay1.of(() -> 50, 6, 50, () -> step[0]);

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
        Delay1 formula = Delay1.of(() -> input[0], 6, 50, () -> step[0]);

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
    public void shouldRespondFasterThanDelay3() {
        int[] step1 = {0};
        int[] step3 = {0};
        double[] input = {0};
        Delay1 delay1 = Delay1.of(() -> input[0], 6, 0, () -> step1[0]);
        Delay3 delay3 = Delay3.of(() -> input[0], 6, 0, () -> step3[0]);

        delay1.getCurrentValue(); // initialize
        delay3.getCurrentValue(); // initialize

        // Apply a step input of 100 and advance a few steps
        input[0] = 100;
        for (int i = 1; i <= 3; i++) {
            step1[0] = i;
            step3[0] = i;
            delay1.getCurrentValue();
            delay3.getCurrentValue();
        }

        double delay1Output = delay1.getCurrentValue();
        double delay3Output = delay3.getCurrentValue();

        // DELAY1 should respond more quickly than DELAY3 after a few steps
        assertTrue(delay1Output > delay3Output,
                "DELAY1 (" + delay1Output + ") should respond faster than DELAY3 ("
                        + delay3Output + ") after 3 steps");
    }

    @Test
    public void shouldProduceDelayedResponse() {
        int[] step = {0};
        double[] input = {0};
        Delay1 formula = Delay1.of(() -> input[0], 6, 0, () -> step[0]);

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

        // Early output should be less than late output (delay effect)
        assertTrue(early < late, "Early output should be less than late output due to delay");
    }

    @Test
    public void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Delay1 formula = Delay1.of(() -> 100, 6, 50, () -> step[0]);

        double first = formula.getCurrentValue();
        double second = formula.getCurrentValue();
        assertEquals(first, second, 0.0);
    }

    @Test
    public void shouldReadInputEachIterationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Delay1 formula = Delay1.of(() -> { readCount[0]++; return 100; }, 6, 100, () -> step[0]);

        formula.getCurrentValue(); // initialize
        readCount[0] = 0;

        // Jump by 5 steps — input should be read once per catch-up iteration
        step[0] = 5;
        formula.getCurrentValue();
        assertEquals(5, readCount[0],
                "Input should be read once per catch-up iteration");
    }

    @Test
    public void shouldRejectNonPositiveDelayTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Delay1.of(() -> 100, 0, () -> 0));
        assertThrows(IllegalArgumentException.class, () ->
                Delay1.of(() -> 100, -1, () -> 0));
    }

    @Test
    public void shouldResetToUninitialized() {
        int[] step = {0};
        double[] input = {100};
        Delay1 formula = Delay1.of(() -> input[0], 6, () -> step[0]);

        formula.getCurrentValue(); // initialize with input=100

        // Change input and advance
        input[0] = 200;
        step[0] = 5;
        formula.getCurrentValue();
        double beforeReset = formula.getCurrentValue();

        // Reset and re-initialize
        formula.reset();
        step[0] = 0;
        input[0] = 50;
        double afterReset = formula.getCurrentValue();

        // After reset, should re-initialize with current input (50)
        assertEquals(50, afterReset, 0.01);
        assertTrue(beforeReset != afterReset, "Reset should change the output");
    }

    @Test
    public void shouldConserveMaterial() {
        // For a constant input, the output should equal the input at steady state
        // (material conservation property of material delays)
        int[] step = {0};
        Delay1 formula = Delay1.of(() -> 100, 4, 100, () -> step[0]);

        formula.getCurrentValue(); // initialize

        for (int i = 1; i <= 100; i++) {
            step[0] = i;
            formula.getCurrentValue();
        }
        // At steady state, output rate should equal input rate
        assertEquals(100, formula.getCurrentValue(), 0.01,
                "At steady state, output should equal input (material conservation)");
    }
}
