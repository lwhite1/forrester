package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TimeSeries")
class TimeSeriesTest {

    private static final double[] TIMES = {0.0, 1.0, 2.0, 3.0, 4.0};
    private static final double[] VALUES = {10.0, 20.0, 30.0, 25.0, 15.0};

    @Nested
    @DisplayName("Linear interpolation")
    class LinearInterpolation {

        @Test
        void shouldReturnExactValueAtDataPoint() {
            double[] time = {0};
            TimeSeries ts = TimeSeries.linear(TIMES, VALUES, () -> time[0]);

            time[0] = 0.0;
            assertThat(ts.getCurrentValue()).isEqualTo(10.0);
            time[0] = 2.0;
            assertThat(ts.getCurrentValue()).isEqualTo(30.0);
            time[0] = 4.0;
            assertThat(ts.getCurrentValue()).isEqualTo(15.0);
        }

        @Test
        void shouldInterpolateLinearly() {
            double[] time = {0};
            TimeSeries ts = TimeSeries.linear(TIMES, VALUES, () -> time[0]);

            time[0] = 0.5;
            assertThat(ts.getCurrentValue()).isCloseTo(15.0, within(1e-10));
            time[0] = 1.5;
            assertThat(ts.getCurrentValue()).isCloseTo(25.0, within(1e-10));
        }

        @Test
        void shouldHoldFirstValueBeforeRange() {
            double[] time = {-1.0};
            TimeSeries ts = TimeSeries.linear(TIMES, VALUES, () -> time[0]);
            assertThat(ts.getCurrentValue()).isEqualTo(10.0);
        }

        @Test
        void shouldHoldLastValueAfterRange() {
            double[] time = {10.0};
            TimeSeries ts = TimeSeries.linear(TIMES, VALUES, () -> time[0]);
            assertThat(ts.getCurrentValue()).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("Step interpolation")
    class StepInterpolation {

        @Test
        void shouldReturnValueAtLowerBound() {
            double[] time = {0};
            TimeSeries ts = TimeSeries.step(TIMES, VALUES, () -> time[0]);

            time[0] = 0.5;
            assertThat(ts.getCurrentValue()).isEqualTo(10.0);
            time[0] = 1.9;
            assertThat(ts.getCurrentValue()).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("Zero extrapolation")
    class ZeroExtrapolation {

        @Test
        void shouldReturnZeroOutsideRange() {
            double[] time = {0};
            TimeSeries ts = TimeSeries.create(TIMES, VALUES, () -> time[0],
                    "LINEAR", "ZERO");

            time[0] = -1.0;
            assertThat(ts.getCurrentValue()).isEqualTo(0.0);
            time[0] = 5.0;
            assertThat(ts.getCurrentValue()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        void shouldRejectMismatchedArrayLengths() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{0, 1, 2}, new double[]{10, 20}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same length");
        }

        @Test
        void shouldRejectEmptyArrays() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{}, new double[]{}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least 2");
        }

        @Test
        void shouldRejectSinglePoint() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{1.0}, new double[]{10.0}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least 2");
        }

        @Test
        void shouldRejectNonAscendingTimeValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{0, 2, 1}, new double[]{10, 20, 30}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("strictly ascending");
        }

        @Test
        void shouldRejectDuplicateTimeValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{0, 1, 1}, new double[]{10, 20, 30}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("strictly ascending");
        }

        @Test
        void shouldRejectNaNInTimeValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{0, Double.NaN}, new double[]{10, 20}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("time values must be finite");
        }

        @Test
        void shouldRejectInfinityInTimeValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(
                            new double[]{0, Double.POSITIVE_INFINITY},
                            new double[]{10, 20}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("time values must be finite");
        }

        @Test
        void shouldRejectNaNInDataValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(new double[]{0, 1}, new double[]{10, Double.NaN}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("data values must be finite");
        }

        @Test
        void shouldRejectNegativeInfinityInDataValues() {
            assertThatThrownBy(() ->
                    TimeSeries.linear(
                            new double[]{0, 1},
                            new double[]{Double.NEGATIVE_INFINITY, 20}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("data values must be finite");
        }

        @Test
        void shouldRejectInvalidInputInStepFactory() {
            assertThatThrownBy(() ->
                    TimeSeries.step(new double[]{}, new double[]{}, () -> 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least 2");
        }

        @Test
        void shouldRejectInvalidInputInCreateFactory() {
            assertThatThrownBy(() ->
                    TimeSeries.create(new double[]{}, new double[]{}, () -> 0,
                            "LINEAR", "HOLD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least 2");
        }

        @Test
        void shouldAcceptValidMinimalInput() {
            double[] time = {0};
            TimeSeries ts = TimeSeries.linear(
                    new double[]{0, 1}, new double[]{10, 20}, () -> time[0]);
            time[0] = 0.5;
            assertThat(ts.getCurrentValue()).isCloseTo(15.0, within(1e-10));
        }
    }
}
