package systems.courant.shrewd.model;

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
    void shouldReadInputEachIterationInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Trend trend = Trend.of(() -> { readCount[0]++; return 100; }, 5, 0, () -> step[0]);

        trend.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should be read once per catch-up iteration
        step[0] = 5;
        trend.getCurrentValue();
        assertEquals(5, readCount[0],
                "Input should be read once per catch-up iteration");
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
}
