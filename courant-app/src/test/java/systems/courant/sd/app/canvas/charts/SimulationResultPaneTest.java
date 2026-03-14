package systems.courant.sd.app.canvas.charts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationResultPane")
class SimulationResultPaneTest {

    @Nested
    @DisplayName("niceTickUnit")
    class NiceTickUnit {

        @Test
        @DisplayName("should return 1 for range 0 or negative")
        void shouldReturnOneForZeroRange() {
            assertThat(SimulationResultPane.niceTickUnit(0)).isEqualTo(1);
            assertThat(SimulationResultPane.niceTickUnit(-5)).isEqualTo(1);
        }

        @Test
        @DisplayName("should return reasonable tick for range 100")
        void shouldComputeTickForRange100() {
            double tick = SimulationResultPane.niceTickUnit(100);
            assertThat(tick).isGreaterThan(0);
            assertThat(tick).isLessThanOrEqualTo(20);
        }

        @Test
        @DisplayName("should return reasonable tick for range 1000")
        void shouldComputeTickForRange1000() {
            double tick = SimulationResultPane.niceTickUnit(1000);
            assertThat(tick).isGreaterThan(0);
            assertThat(tick).isLessThanOrEqualTo(200);
        }

        @Test
        @DisplayName("should return reasonable tick for range 1")
        void shouldComputeTickForRange1() {
            double tick = SimulationResultPane.niceTickUnit(1);
            assertThat(tick).isGreaterThan(0);
            assertThat(tick).isLessThanOrEqualTo(0.5);
        }

        @Test
        @DisplayName("should return reasonable tick for range 50")
        void shouldComputeTickForRange50() {
            double tick = SimulationResultPane.niceTickUnit(50);
            assertThat(tick).isGreaterThan(0);
            // ~5 ticks means tick unit around 5-10
            assertThat(tick).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("formatTimeStep")
    class FormatTimeStep {

        @Test
        @DisplayName("should display whole numbers without decimal point")
        void shouldDisplayWholeNumbersAsIntegers() {
            assertThat(SimulationResultPane.formatTimeStep(0.0)).isEqualTo("0");
            assertThat(SimulationResultPane.formatTimeStep(1.0)).isEqualTo("1");
            assertThat(SimulationResultPane.formatTimeStep(10.0)).isEqualTo("10");
            assertThat(SimulationResultPane.formatTimeStep(100.0)).isEqualTo("100");
        }

        @Test
        @DisplayName("should display fractional steps with appropriate precision")
        void shouldDisplayFractionalSteps() {
            assertThat(SimulationResultPane.formatTimeStep(0.25)).isEqualTo("0.25");
            assertThat(SimulationResultPane.formatTimeStep(0.5)).isEqualTo("0.5");
            assertThat(SimulationResultPane.formatTimeStep(0.75)).isEqualTo("0.75");
            assertThat(SimulationResultPane.formatTimeStep(1.25)).isEqualTo("1.25");
        }

        @Test
        @DisplayName("should strip trailing zeros from fractional values")
        void shouldStripTrailingZeros() {
            assertThat(SimulationResultPane.formatTimeStep(0.5)).isEqualTo("0.5");
            assertThat(SimulationResultPane.formatTimeStep(2.10)).isEqualTo("2.1");
            assertThat(SimulationResultPane.formatTimeStep(3.500)).isEqualTo("3.5");
        }

        @Test
        @DisplayName("should handle negative time values")
        void shouldHandleNegativeValues() {
            assertThat(SimulationResultPane.formatTimeStep(-1.0)).isEqualTo("-1");
            assertThat(SimulationResultPane.formatTimeStep(-0.25)).isEqualTo("-0.25");
        }

        @Test
        @DisplayName("should preserve up to 4 decimal places for high-precision steps")
        void shouldPreserveUpToFourDecimalPlaces() {
            assertThat(SimulationResultPane.formatTimeStep(0.1234)).isEqualTo("0.1234");
            assertThat(SimulationResultPane.formatTimeStep(0.0001)).isEqualTo("0.0001");
        }

        @Test
        @DisplayName("should handle typical DT=0.25 simulation sequence")
        void shouldHandleQuarterStepSequence() {
            assertThat(SimulationResultPane.formatTimeStep(0.0)).isEqualTo("0");
            assertThat(SimulationResultPane.formatTimeStep(0.25)).isEqualTo("0.25");
            assertThat(SimulationResultPane.formatTimeStep(0.50)).isEqualTo("0.5");
            assertThat(SimulationResultPane.formatTimeStep(0.75)).isEqualTo("0.75");
            assertThat(SimulationResultPane.formatTimeStep(1.0)).isEqualTo("1");
            assertThat(SimulationResultPane.formatTimeStep(1.25)).isEqualTo("1.25");
        }
    }
}
