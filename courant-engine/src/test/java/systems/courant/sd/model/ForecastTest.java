package systems.courant.sd.model;

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
    void shouldReadInputOncePerCatchUpCall() {
        int[] step = {0};
        int[] readCount = {0};
        Forecast forecast = Forecast.of(() -> { readCount[0]++; return 100; }, 5, 3, 0, () -> step[0]);

        forecast.getCurrentValue(); // initialize (1 read)
        readCount[0] = 0;

        // Jump by 5 steps — input should be read once (zero-order hold for intermediate steps)
        step[0] = 5;
        forecast.getCurrentValue();
        assertEquals(1, readCount[0],
                "Input should be read once per getCurrentValue call (zero-order hold)");
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

    @Test
    void shouldSmoothAtCorrectRateWithSubUnitDt() {
        // With DT=0.25, four integration steps should approximate one DT=1.0 step.
        // Use initialTrend=0.1 so the averaging actually does work.
        int[] step1 = {0};
        Forecast fcDt1 = Forecast.of(() -> 100, 5, 3, 0.1, () -> step1[0]);
        fcDt1.getCurrentValue();

        int[] step025 = {0};
        double[] dt = {0.25};
        Forecast fcDt025 = Forecast.of(() -> 100, 5, 3, 0.1, dt, () -> step025[0]);
        fcDt025.getCurrentValue();

        step1[0] = 1;
        double val1 = fcDt1.getCurrentValue();
        for (int i = 1; i <= 4; i++) {
            step025[0] = i;
            fcDt025.getCurrentValue();
        }
        double val025 = fcDt025.getCurrentValue();
        assertEquals(val1, val025, 1.0,
                "Forecast with DT=0.25 over 4 steps should approximate DT=1.0 over 1 step");
    }

    @Test
    void shouldNotProduceExtremeValuesWhenAverageInputNearZero() {
        int[] step = {0};
        double[] inputVal = {0.01};
        Forecast forecast = Forecast.of(() -> inputVal[0], 5, 3, 0, () -> step[0]);
        forecast.getCurrentValue(); // initialize

        for (int i = 1; i <= 20; i++) {
            step[0] = i;
            inputVal[0] = 0.01 * Math.sin(i * 0.5);
            double val = forecast.getCurrentValue();
            assertEquals(false, Double.isNaN(val), "Forecast should not be NaN at step " + i);
            assertEquals(false, Double.isInfinite(val),
                    "Forecast should not be Infinite at step " + i);
            assertEquals(true, Math.abs(val) < 100,
                    "Forecast should not be extreme at step " + i + ", was " + val);
        }
    }
}
