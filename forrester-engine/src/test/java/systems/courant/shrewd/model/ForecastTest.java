package systems.courant.shrewd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForecastTest {

    @Test
    void shouldReturnConsistentValueWithinSameStep() {
        int[] step = {0};
        double[] inputVal = {100};
        Forecast forecast = Forecast.of(() -> inputVal[0], 5, 3, 0, () -> step[0]);

        // Initialize at step 0
        double first = forecast.getCurrentValue();

        // Change the input — but since step hasn't advanced, the forecast should
        // use the cached input value, not re-read the (now different) supplier
        inputVal[0] = 999;
        double second = forecast.getCurrentValue();

        assertEquals(first, second, 0.0,
                "Forecast should return same value within same timestep, "
                        + "not re-read the input supplier");
    }

    @Test
    void shouldInitializeFromFirstInput() {
        int[] step = {0};
        Forecast forecast = Forecast.of(() -> 100, 5, 3, 0, () -> step[0]);

        // With initialTrend=0, forecast = input * (1 + 0 * horizon) = input
        assertEquals(100, forecast.getCurrentValue(), 0.01);
    }

    @Test
    void shouldExtrapolateWithPositiveTrend() {
        int[] step = {0};
        double[] input = {100};
        Forecast forecast = Forecast.of(() -> input[0], 5, 3, 0.1, () -> step[0]);

        // With initialTrend=0.1 and horizon=3: forecast = 100 * (1 + 0.1 * 3) = 130
        assertEquals(130, forecast.getCurrentValue(), 0.01);
    }

    @Test
    void shouldReadInputOncePerStepInCatchUpLoop() {
        int[] step = {0};
        int[] readCount = {0};
        Forecast forecast = Forecast.of(() -> { readCount[0]++; return 100; }, 5, 3, 0, () -> step[0]);

        forecast.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should only be read once, not 5 times
        step[0] = 5;
        forecast.getCurrentValue();
        assertEquals(1, readCount[0],
                "Input should be read exactly once per step advance, not once per delta iteration");
    }

    @Test
    void shouldUpdateOnNewStep() {
        int[] step = {0};
        double[] input = {100};
        Forecast forecast = Forecast.of(() -> input[0], 5, 3, 0, () -> step[0]);

        forecast.getCurrentValue(); // initialize

        // Advance step with increasing input
        input[0] = 110;
        step[0] = 1;
        double val = forecast.getCurrentValue();

        // Trend should now be positive, forecast > input
        assertEquals(true, val > 110,
                "Forecast should extrapolate above current input when trend is positive");
    }
}
