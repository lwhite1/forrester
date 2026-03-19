package systems.courant.sd.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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

    @Test
    void shouldCallStreamOnlyOncePerCatchUp() {
        int[] step = {0};
        AtomicInteger callCount = new AtomicInteger(0);
        Npv npv = Npv.of(() -> {
            callCount.incrementAndGet();
            return 100;
        }, 0.1, () -> step[0]);

        npv.getCurrentValue(); // step 0 — 1 call
        assertThat(callCount.get()).isEqualTo(1);

        step[0] = 3; // skip ahead by 3 steps
        npv.getCurrentValue();
        // Should call stream only once more (not 3 times)
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void shouldAccumulatePaymentAtEachSubStepWhenSkipping() {
        int[] step1 = {0};
        int[] step2 = {0};
        Npv stepped = Npv.of(() -> 100, 0.1, () -> step1[0]);
        Npv skipped = Npv.of(() -> 100, 0.1, () -> step2[0]);

        // Step through one at a time: adds payment at each step
        stepped.getCurrentValue(); // step 0
        step1[0] = 1;
        stepped.getCurrentValue();
        step1[0] = 2;
        stepped.getCurrentValue();
        step1[0] = 3;
        double steppedVal = stepped.getCurrentValue();

        // Skip directly from 0 to 3: should accumulate payments at each sub-step
        skipped.getCurrentValue(); // step 0
        step2[0] = 3;
        double skippedVal = skipped.getCurrentValue();

        // With constant stream, skipped should equal stepped (zero-order hold)
        assertThat(skippedVal).isCloseTo(steppedVal, within(1e-9));
    }
}
