package systems.courant.shrewd.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Smooth3Test {

    @Test
    void shouldReturnInitialValueAtStepZero() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 100, 6, 50, () -> step[0]);
        assertThat(formula.getCurrentValue()).isEqualTo(50);
    }

    @Test
    void shouldDefaultInitialToFirstInput() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 100, 6, () -> step[0]);
        assertThat(formula.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void shouldApproachInputOverTime() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 100, 6, 0, () -> step[0]);

        assertThat(formula.getCurrentValue()).isEqualTo(0);

        double prev = 0;
        for (int i = 1; i <= 30; i++) {
            step[0] = i;
            double val = formula.getCurrentValue();
            assertThat(val).as("Step %d should be increasing", i).isGreaterThan(prev);
            prev = val;
        }
        assertThat(prev).as("Should have converged near input after 30 steps").isGreaterThan(95);
    }

    @Test
    void shouldProduceDifferentResponseThanFirstOrderSmooth() {
        int[] step = {0};
        Smooth smooth1 = Smooth.of(() -> 100, 6, 0, () -> step[0]);
        Smooth3 smooth3 = Smooth3.of(() -> 100, 6, 0, () -> step[0]);

        smooth1.getCurrentValue();
        smooth3.getCurrentValue();

        // Advance through multiple steps and verify the trajectories differ.
        // SMOOTH3 has an S-shaped response that crosses the first-order SMOOTH curve.
        boolean foundDifference = false;
        for (int i = 1; i <= 20; i++) {
            step[0] = i;
            double val1 = smooth1.getCurrentValue();
            double val3 = smooth3.getCurrentValue();
            if (Math.abs(val1 - val3) > 0.5) {
                foundDifference = true;
            }
        }
        assertThat(foundDifference).as("SMOOTH3 and SMOOTH should produce different trajectories")
                .isTrue();
    }

    @Test
    void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 100, 6, 50, () -> step[0]);

        double first = formula.getCurrentValue();
        double second = formula.getCurrentValue();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldRemainStableWhenInputEqualsSmoothed() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 50, 6, 50, () -> step[0]);

        formula.getCurrentValue();
        step[0] = 1;
        assertThat(formula.getCurrentValue()).isCloseTo(50, org.assertj.core.data.Offset.offset(0.01));
        step[0] = 10;
        assertThat(formula.getCurrentValue()).isCloseTo(50, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void shouldReadInputOncePerEvaluationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Smooth3 formula = Smooth3.of(() -> { readCount[0]++; return 100; }, 6, 0, () -> step[0]);

        formula.getCurrentValue();
        readCount[0] = 0;

        step[0] = 5;
        formula.getCurrentValue();
        assertThat(readCount[0]).as("Input should be read once per evaluation, not once per sub-step")
                .isEqualTo(1);
    }

    @Test
    void shouldResetToUninitialized() {
        int[] step = {0};
        Smooth3 formula = Smooth3.of(() -> 100, 6, 0, () -> step[0]);

        formula.getCurrentValue();
        step[0] = 10;
        double afterRun = formula.getCurrentValue();
        assertThat(afterRun).isGreaterThan(0);

        formula.reset();
        step[0] = 0;
        assertThat(formula.getCurrentValue()).as("After reset, should re-initialize from explicit initial")
                .isEqualTo(0);
    }

    @Test
    void shouldRejectNonPositiveSmoothingTime() {
        assertThatThrownBy(() -> Smooth3.of(() -> 100, 0, () -> 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Smooth3.of(() -> 100, -1, () -> 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
