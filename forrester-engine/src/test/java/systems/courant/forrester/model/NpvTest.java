package systems.courant.forrester.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpvTest {

    @Test
    void shouldRejectDiscountRateOfNegativeOne() {
        assertThrows(IllegalArgumentException.class, () ->
                Npv.of(() -> 100, -1.0, () -> 0));
    }

    @Test
    void shouldRejectDiscountRateBelowNegativeOne() {
        assertThrows(IllegalArgumentException.class, () ->
                Npv.of(() -> 100, -2.0, () -> 0));
    }

    @Test
    void shouldAcceptZeroDiscountRate() {
        int[] step = {0};
        Npv npv = Npv.of(() -> 100, 0.0, () -> step[0]);
        assertEquals(100, npv.getCurrentValue(), 0.01);
        step[0] = 1;
        assertEquals(200, npv.getCurrentValue(), 0.01);
    }

    @Test
    void shouldAccumulateDiscountedValues() {
        int[] step = {0};
        Npv npv = Npv.of(() -> 100, 0.1, () -> step[0]);

        // Step 0: accumulated = 100 * 1 = 100 (no discounting on first step)
        assertEquals(100, npv.getCurrentValue(), 0.01);

        // Step 1: accumulated += 100 / 1.1 = 100 + 90.91 = 190.91
        step[0] = 1;
        double val = npv.getCurrentValue();
        assertTrue(val > 190 && val < 192, "Expected ~190.91 but got " + val);
    }

    @Test
    void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Npv npv = Npv.of(() -> 100, 0.05, () -> step[0]);
        double first = npv.getCurrentValue();
        double second = npv.getCurrentValue();
        assertEquals(first, second, 0.0);
    }
}
