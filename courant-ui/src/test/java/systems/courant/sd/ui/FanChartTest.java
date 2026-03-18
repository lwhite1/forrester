package systems.courant.sd.ui;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FanChart")
@ExtendWith(ApplicationExtension.class)
class FanChartTest {

    @Start
    void start(Stage stage) {
        // JavaFX toolkit initialization only
    }

    @Test
    @DisplayName("no-arg constructor leaves result null (#533)")
    void noArgConstructorLeavesResultNull() {
        FanChart chart = new FanChart();
        assertThat(chart).isNotNull();
    }

    @Test
    @DisplayName("start() with no-arg constructor throws IllegalStateException (#533)")
    void startWithNoArgConstructorThrowsIllegalState() {
        CompletableFuture<Throwable> thrown = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                FanChart chart = new FanChart();
                chart.start(new Stage());
                thrown.complete(null);
            } catch (Throwable t) {
                thrown.complete(t);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        Throwable result = thrown.join();
        assertThat(result)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FanChart requires a MonteCarloResult");
    }

    @Nested
    @DisplayName("padAxisRange (#864)")
    class PadAxisRangeTest {

        @Test
        @DisplayName("non-zero range adds 5% padding on each side")
        void shouldAdd5PercentPaddingForNonZeroRange() {
            double[] result = FanChart.padAxisRange(100, 200);

            assertThat(result[0]).isCloseTo(95.0, within(1e-9));
            assertThat(result[1]).isCloseTo(205.0, within(1e-9));
        }

        @Test
        @DisplayName("constant non-zero value centers range at +/-10% of magnitude (#864)")
        void shouldCenterRangeAroundConstantNonZeroValue() {
            double[] result = FanChart.padAxisRange(1000, 1000);

            // halfRange = |1000| * 0.1 = 100
            assertThat(result[0]).isCloseTo(900.0, within(1e-9));
            assertThat(result[1]).isCloseTo(1100.0, within(1e-9));
        }

        @Test
        @DisplayName("constant zero value uses +/-1 range (#864)")
        void shouldUseUnitRangeForConstantZero() {
            double[] result = FanChart.padAxisRange(0, 0);

            assertThat(result[0]).isCloseTo(-1.0, within(1e-9));
            assertThat(result[1]).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("constant negative value centers range at +/-10% of magnitude (#864)")
        void shouldCenterRangeAroundConstantNegativeValue() {
            double[] result = FanChart.padAxisRange(-500, -500);

            // halfRange = |-500| * 0.1 = 50
            assertThat(result[0]).isCloseTo(-550.0, within(1e-9));
            assertThat(result[1]).isCloseTo(-450.0, within(1e-9));
        }

        @Test
        @DisplayName("small constant value scales proportionally (#864)")
        void shouldScaleProportionallyForSmallConstant() {
            double[] result = FanChart.padAxisRange(0.001, 0.001);

            // halfRange = 0.001 * 0.1 = 0.0001
            assertThat(result[0]).isCloseTo(0.0009, within(1e-12));
            assertThat(result[1]).isCloseTo(0.0011, within(1e-12));
        }

        @Test
        @DisplayName("large constant value scales proportionally (#864)")
        void shouldScaleProportionallyForLargeConstant() {
            double[] result = FanChart.padAxisRange(1_000_000, 1_000_000);

            // halfRange = 1_000_000 * 0.1 = 100_000
            assertThat(result[0]).isCloseTo(900_000.0, within(1e-6));
            assertThat(result[1]).isCloseTo(1_100_000.0, within(1e-6));
        }

        @Test
        @DisplayName("padded min is always less than padded max")
        void shouldAlwaysProducePaddedMinLessThanPaddedMax() {
            double[][] testCases = {
                    {0, 0}, {1000, 1000}, {-500, -500}, {0.001, 0.001},
                    {100, 200}, {-200, -100}, {-100, 100}
            };
            for (double[] tc : testCases) {
                double[] result = FanChart.padAxisRange(tc[0], tc[1]);
                assertThat(result[0])
                        .as("padAxisRange(%.1f, %.1f): min < max", tc[0], tc[1])
                        .isLessThan(result[1]);
            }
        }
    }
}
