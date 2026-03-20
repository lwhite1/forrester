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
    void shouldSmoothAtCorrectRateWithSubUnitDt() {
        // With DT=0.25, four integration steps should produce the same result
        // as one DT=1.0 step. Use initialTrend=0.1 so the averaging actually does work
        // (with zero initial trend and constant input, both sides are trivially 0).
        int[] step1 = {0};
        Trend trendDt1 = Trend.of(() -> 100, 5, 0.1, () -> step1[0]);
        trendDt1.getCurrentValue(); // initialize

        int[] step025 = {0};
        double[] dt = {0.25};
        Trend trendDt025 = Trend.of(() -> 100, 5, 0.1, dt, () -> step025[0]);
        trendDt025.getCurrentValue(); // initialize

        // Advance DT=1 by 1 step, DT=0.25 by 4 steps — should approximately match
        step1[0] = 1;
        double val1 = trendDt1.getCurrentValue();
        for (int i = 1; i <= 4; i++) {
            step025[0] = i;
            trendDt025.getCurrentValue();
        }
        double val025 = trendDt025.getCurrentValue();
        assertEquals(val1, val025, 0.005,
                "Trend with DT=0.25 over 4 steps should approximate DT=1.0 over 1 step");
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
