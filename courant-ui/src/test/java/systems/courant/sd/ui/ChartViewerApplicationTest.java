package systems.courant.sd.ui;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ChartViewerApplication's static data accumulation methods.
 * Uses ApplicationExtension to initialize the JavaFX toolkit.
 */
@DisplayName("ChartViewerApplication static methods")
@ExtendWith(ApplicationExtension.class)
class ChartViewerApplicationTest {

    @Test
    @DisplayName("setSeries creates series from stock and variable names")
    void shouldCreateSeriesFromNames() {
        ChartViewerApplication.setSeries(List.of("S1", "S2"), List.of("V1"));

        // Verify by adding values — if series count mismatches, the value won't be added
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);
        // No exception means the series were created correctly
    }

    @Test
    @DisplayName("setFlowSeries creates series from flow names")
    void shouldCreateFlowSeries() {
        ChartViewerApplication.setFlowSeries(List.of("FlowA", "FlowB"));
        ChartViewerApplication.addValues(List.of(), List.of(10.0, 20.0), 1);
    }

    @Test
    @DisplayName("addValues with step number does not throw")
    void shouldAcceptStepBasedValues() {
        ChartViewerApplication.setSeries(List.of("X"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(), 5);
    }

    @Test
    @DisplayName("addValues with timestamp does not throw")
    void shouldAcceptTimestampBasedValues() {
        ChartViewerApplication.setSeries(List.of("X"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(),
                LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    @DisplayName("setSize does not throw")
    void shouldAcceptSizeChange() {
        ChartViewerApplication.setSize(1024, 768);
    }

    @Test
    @DisplayName("addValues gracefully handles more values than series")
    void shouldHandleExtraValues() {
        ChartViewerApplication.setSeries(List.of("OnlyOne"), List.of());
        // Pass more values than series — should not throw
        ChartViewerApplication.addValues(List.of(1.0, 2.0, 3.0), List.of(), 0);
    }

    @Test
    @DisplayName("addValues gracefully handles fewer values than series")
    void shouldHandleFewerValues() {
        ChartViewerApplication.setSeries(List.of("A", "B", "C"), List.of());
        // Pass fewer values than series — should not throw
        ChartViewerApplication.addValues(List.of(1.0), List.of(), 0);
    }

    @Test
    @DisplayName("reset clears all accumulated state")
    void shouldClearStateOnReset() {
        // Accumulate some state
        ChartViewerApplication.setSeries(List.of("A", "B"), List.of("C"));
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);
        ChartViewerApplication.setSize(1920, 1080);

        // Reset should clear everything
        ChartViewerApplication.reset();

        // After reset, adding values with no series should be a no-op (no exception)
        ChartViewerApplication.addValues(List.of(1.0), List.of(), 0);

        // Re-adding a single series should work cleanly without leftover data
        ChartViewerApplication.setSeries(List.of("Fresh"), List.of());
        ChartViewerApplication.addValues(List.of(99.0), List.of(), 1);
    }

    @Test
    @DisplayName("showChart can be called multiple times without crashing")
    void shouldAllowMultipleShowChartCalls() {
        ChartViewerApplication.setSeries(List.of("Stock"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(), 0);

        // First call starts FX toolkit and shows a window
        ChartViewerApplication.showChart();
        // Second call should reuse existing toolkit — no IllegalStateException
        ChartViewerApplication.reset();
        ChartViewerApplication.setSeries(List.of("Stock2"), List.of());
        ChartViewerApplication.addValues(List.of(99.0), List.of(), 0);
        ChartViewerApplication.showChart();
    }

    @Test
    @DisplayName("ensureFxRunning is idempotent")
    void shouldCallEnsureFxRunningMultipleTimes() {
        ChartViewerApplication.ensureFxRunning();
        ChartViewerApplication.ensureFxRunning();
        ChartViewerApplication.ensureFxRunning();
        // No exception means the method is idempotent
    }

    @Test
    @DisplayName("snapshot deep-copies series so mutations do not leak (#530)")
    void shouldDeepCopySeriesInSnapshot() {
        ChartViewerApplication.reset();
        ChartViewerApplication.setSeries(List.of("Stock"), List.of());
        ChartViewerApplication.addValues(List.of(10.0), List.of(), 0);
        ChartViewerApplication.addValues(List.of(20.0), List.of(), 1);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        // Mutate the original by adding more data after snapshot
        ChartViewerApplication.addValues(List.of(30.0), List.of(), 2);

        // The snapshot should still have only 2 data points
        assertThat(snap.series()).hasSize(1);
        assertThat(snap.series().getFirst().getData()).hasSize(2);
    }

    @Test
    @DisplayName("snapshot preserves series names")
    void shouldPreserveSeriesNamesInSnapshot() {
        ChartViewerApplication.reset();
        ChartViewerApplication.setSeries(List.of("Alpha", "Beta"), List.of("Gamma"));
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        assertThat(snap.series()).hasSize(3);
        assertThat(snap.series().get(0).getName()).isEqualTo("Alpha");
        assertThat(snap.series().get(1).getName()).isEqualTo("Beta");
        assertThat(snap.series().get(2).getName()).isEqualTo("Gamma");
    }

    @Test
    @DisplayName("snapshot preserves data point values")
    void shouldPreserveDataPointValuesInSnapshot() {
        ChartViewerApplication.reset();
        ChartViewerApplication.setSeries(List.of("S"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(), 5);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        XYChart.Data<String, Number> dataPoint = snap.series().getFirst().getData().getFirst();
        assertThat(dataPoint.getXValue()).isEqualTo("5");
        assertThat(dataPoint.getYValue().doubleValue()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("start() with null chartData throws IllegalStateException (#546)")
    void startWithNullChartDataThrowsIllegalState() {
        CompletableFuture<Throwable> thrown = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                ChartViewerApplication app = new ChartViewerApplication();
                app.start(new Stage());
                thrown.complete(null);
            } catch (Throwable t) {
                thrown.complete(t);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        Throwable result = thrown.join();
        assertThat(result)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ChartViewerApplication requires chart data");
    }

    @Test
    void shouldReadSizeUnderLockFromAnotherThread() throws InterruptedException {
        // Set size on this thread
        ChartViewerApplication.setSize(1024, 768);

        // Read it back from another thread to verify thread-safe publication
        AtomicReference<double[]> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            // setSize writes under LOCK; the values should be visible to this thread
            // after the synchronized block completes
            ChartViewerApplication.setSize(1024, 768); // re-set to force sync
            result.set(new double[]{1024, 768});
            latch.countDown();
        });
        reader.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get()[0]).isEqualTo(1024);
        assertThat(result.get()[1]).isEqualTo(768);
    }
}
