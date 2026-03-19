package systems.courant.sd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrendTest {

    @Test
    void shouldReturnInitialTrendAtStepZero() {
        int[] step = {0};
        Trend trend = Trend.of(() -> 100, 5, 0.1, () -> step[0]);
        assertEquals(0.1, trend.getCurrentValue(), 0.001);
    }

    @Test
    void shouldReturnZeroTrendForConstantInput() {
        int[] step = {0};
        Trend trend = Trend.of(() -> 100, 5, 0, () -> step[0]);

        trend.getCurrentValue(); // initialize

        for (int i = 1; i <= 10; i++) {
            step[0] = i;
            assertEquals(0, trend.getCurrentValue(), 0.001,
                    "Trend should be zero for constant input at step " + i);
        }
    }

    @Test
    void shouldReadInputOncePerCatchUpCall() {
        int[] step = {0};
        int[] readCount = {0};
        Trend trend = Trend.of(() -> { readCount[0]++; return 100; }, 5, 0, () -> step[0]);

        trend.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should be read once (zero-order hold for intermediate steps)
        step[0] = 5;
        trend.getCurrentValue();
        assertEquals(1, readCount[0],
                "Input should be read once per getCurrentValue call (zero-order hold)");
    }

    @Test
    void shouldReturnSameValueWithinSameStep() {
        int[] step = {0};
        Trend trend = Trend.of(() -> 100, 5, 0, () -> step[0]);
        double first = trend.getCurrentValue();
        double second = trend.getCurrentValue();
        assertEquals(first, second, 0.0);
    }

    @Test
    void shouldRejectNonPositiveAveragingTime() {
        assertThrows(IllegalArgumentException.class, () ->
                Trend.of(() -> 100, 0, 0, () -> 0));
        assertThrows(IllegalArgumentException.class, () ->
                Trend.of(() -> 100, -1, 0, () -> 0));
    }

    @Test
    void shouldNotProduceExtremeValuesWhenAverageInputNearZero() {
        int[] step = {0};
        // Input that oscillates around zero: starts at a small positive value
        // and crosses through zero
        double[] inputVal = {0.01};
        Trend trend = Trend.of(() -> inputVal[0], 5, 0, () -> step[0]);
        trend.getCurrentValue(); // initialize

        // Drive input to near-zero
        for (int i = 1; i <= 20; i++) {
            step[0] = i;
            inputVal[0] = 0.01 * Math.sin(i * 0.5); // crosses zero
            double trendVal = trend.getCurrentValue();
            // Should never produce extreme values (NaN, Infinity, or very large)
            assertEquals(false, Double.isNaN(trendVal), "Trend should not be NaN at step " + i);
            assertEquals(false, Double.isInfinite(trendVal),
                    "Trend should not be Infinite at step " + i);
            assertEquals(true, Math.abs(trendVal) < 100,
                    "Trend should not be extreme at step " + i + ", was " + trendVal);
        }
    }
}
