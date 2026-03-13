package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for validation guards and reset behavior in Formula implementations (T2 from code audit).
 */
@DisplayName("Formula constructor guards and reset")
class FormulaGuardTest {

    private int step = 0;

    @Nested
    @DisplayName("Pulse")
    class PulseGuards {

        @Test
        void shouldRejectNullCurrentStep() {
            assertThatThrownBy(() -> Pulse.of(100, 5, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void shouldRejectNegativeStartStep(int startStep) {
            assertThatThrownBy(() -> Pulse.of(100, startStep, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startStep");
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -5})
        void shouldRejectNegativeInterval(int interval) {
            assertThatThrownBy(() -> Pulse.of(100, 0, interval, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("interval");
        }

        @Test
        void shouldReturnMagnitudeAtStartStep() {
            step = 5;
            Pulse pulse = Pulse.of(42, 5, () -> step);
            assertThat(pulse.getCurrentValue()).isEqualTo(42);
        }

        @Test
        void shouldReturnZeroBeforeStartStep() {
            step = 0;
            Pulse pulse = Pulse.of(42, 5, () -> step);
            assertThat(pulse.getCurrentValue()).isZero();
        }

        @Test
        void shouldReturnZeroAfterSinglePulse() {
            step = 6;
            Pulse pulse = Pulse.of(42, 5, () -> step);
            assertThat(pulse.getCurrentValue()).isZero();
        }

        @Test
        void shouldRepeatAtInterval() {
            Pulse pulse = Pulse.of(10, 2, 3, () -> step);
            step = 2;
            assertThat(pulse.getCurrentValue()).isEqualTo(10);
            step = 5;
            assertThat(pulse.getCurrentValue()).isEqualTo(10);
            step = 8;
            assertThat(pulse.getCurrentValue()).isEqualTo(10);
            step = 4;
            assertThat(pulse.getCurrentValue()).isZero();
        }
    }

    @Nested
    @DisplayName("DelayFixed")
    class DelayFixedGuards {

        @Test
        void shouldRejectNullInput() {
            assertThatThrownBy(() -> DelayFixed.of(null, 5, 0, () -> 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectNullCurrentStep() {
            assertThatThrownBy(() -> DelayFixed.of(() -> 1, 5, 0, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void shouldRejectNonPositiveDelaySteps(int delaySteps) {
            assertThatThrownBy(() -> DelayFixed.of(() -> 1, delaySteps, 0, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("delaySteps");
        }

        @Test
        void shouldReturnInitialValueBeforeDelayElapsed() {
            step = 0;
            DelayFixed delay = DelayFixed.of(() -> 99, 3, 42, () -> step);
            assertThat(delay.getCurrentValue()).isEqualTo(42);
        }

        @Test
        void shouldReturnDelayedValue() {
            double[] input = {10};
            step = 0;
            DelayFixed delay = DelayFixed.of(() -> input[0], 2, 0, () -> step);

            // Step 0: buffer initialized with initial value (0)
            assertThat(delay.getCurrentValue()).isEqualTo(0);
            // Step 1: writes input (10), reads oldest = initial value (0)
            step = 1;
            assertThat(delay.getCurrentValue()).isEqualTo(0);
            // Step 2: writes input (10), reads value written at step 1 = 10
            step = 2;
            assertThat(delay.getCurrentValue()).isEqualTo(10);
        }

        @Test
        void shouldResetToUninitialized() {
            step = 0;
            DelayFixed delay = DelayFixed.of(() -> 99, 2, 42, () -> step);
            delay.getCurrentValue(); // initialize
            delay.reset();
            // After reset, should re-initialize with initial value
            assertThat(delay.getCurrentValue()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("Trend")
    class TrendGuards {

        @Test
        void shouldRejectNullInput() {
            assertThatThrownBy(() -> Trend.of(null, 10, 0, () -> 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectNullCurrentStep() {
            assertThatThrownBy(() -> Trend.of(() -> 1, 10, 0, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1, -100})
        void shouldRejectNonPositiveAveragingTime(double avgTime) {
            assertThatThrownBy(() -> Trend.of(() -> 1, avgTime, 0, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("averagingTime");
        }

        @Test
        void shouldReturnInitialTrendOnFirstCall() {
            step = 0;
            Trend trend = Trend.of(() -> 100, 10, 0.05, () -> step);
            assertThat(trend.getCurrentValue()).isEqualTo(0.05);
        }

        @Test
        void shouldReturnZeroTrendForConstantInput() {
            step = 0;
            Trend trend = Trend.of(() -> 100, 5, 0, () -> step);
            trend.getCurrentValue(); // initialize
            step = 1;
            trend.getCurrentValue();
            step = 10;
            assertThat(trend.getCurrentValue()).isCloseTo(0, org.assertj.core.data.Offset.offset(1e-10));
        }

        @Test
        void shouldResetState() {
            step = 0;
            Trend trend = Trend.of(() -> 100, 10, 0.05, () -> step);
            trend.getCurrentValue();
            step = 5;
            trend.getCurrentValue();
            trend.reset();
            step = 0;
            assertThat(trend.getCurrentValue()).isEqualTo(0.05);
        }
    }

    @Nested
    @DisplayName("Forecast")
    class ForecastGuards {

        @Test
        void shouldRejectNullInput() {
            assertThatThrownBy(() -> Forecast.of(null, 10, 5, 0, () -> 0))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectNullCurrentStep() {
            assertThatThrownBy(() -> Forecast.of(() -> 1, 10, 5, 0, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1, -100})
        void shouldRejectNonPositiveAveragingTime(double avgTime) {
            assertThatThrownBy(() -> Forecast.of(() -> 1, avgTime, 5, 0, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("averagingTime");
        }

        @Test
        void shouldForecastConstantInputAsUnchanged() {
            step = 0;
            Forecast fc = Forecast.of(() -> 100, 10, 5, 0, () -> step);
            // With zero initial trend and constant input, forecast = input
            assertThat(fc.getCurrentValue()).isEqualTo(100);
        }

        @Test
        void shouldResetState() {
            step = 0;
            Forecast fc = Forecast.of(() -> 100, 10, 5, 0.1, () -> step);
            fc.getCurrentValue();
            step = 5;
            fc.getCurrentValue();
            fc.reset();
            step = 0;
            // After reset, should return initial forecast again
            double val = fc.getCurrentValue();
            assertThat(val).isEqualTo(100 * (1 + 0.1 * 5)); // input * (1 + trend * horizon)
        }
    }
}
