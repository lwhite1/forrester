package systems.courant.sd.ui;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ChartViewerApplication's static data accumulation methods.
 * Uses ApplicationExtension to initialize the JavaFX toolkit.
 */
@DisplayName("ChartViewerApplication static methods")
@ExtendWith(ApplicationExtension.class)
class ChartViewerApplicationTest {

    @BeforeEach
    void setUp() {
        ChartViewerApplication.reset();
    }

    @Test
    @DisplayName("setSeries creates series from stock and variable names")
    void shouldCreateSeriesFromNames() {
        ChartViewerApplication.setSeries(List.of("S1", "S2"), List.of("V1"));
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).hasSize(3);
        assertThat(snap.series().get(0).getName()).isEqualTo("S1");
        assertThat(snap.series().get(1).getName()).isEqualTo("S2");
        assertThat(snap.series().get(2).getName()).isEqualTo("V1");
    }

    @Test
    @DisplayName("setFlowSeries creates series from flow names")
    void shouldCreateFlowSeries() {
        ChartViewerApplication.setFlowSeries(List.of("FlowA", "FlowB"));
        ChartViewerApplication.addValues(List.of(), List.of(10.0, 20.0), 1);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).hasSize(2);
        assertThat(snap.series().get(0).getName()).isEqualTo("FlowA");
        assertThat(snap.series().get(1).getName()).isEqualTo("FlowB");
    }

    @Test
    @DisplayName("addValues with step number records data point")
    void shouldAcceptStepBasedValues() {
        ChartViewerApplication.setSeries(List.of("X"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(), 5);

        XYChart.Data<String, Number> dp =
                ChartViewerApplication.snapshot().series().getFirst().getData().getFirst();
        assertThat(dp.getXValue()).isEqualTo("5");
        assertThat(dp.getYValue().doubleValue()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("addValues with timestamp records data point")
    void shouldAcceptTimestampBasedValues() {
        ChartViewerApplication.setSeries(List.of("X"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(),
                LocalDateTime.of(2026, 1, 1, 12, 0));

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series().getFirst().getData()).hasSize(1);
        assertThat(snap.series().getFirst().getData().getFirst().getYValue().doubleValue())
                .isEqualTo(42.0);
    }

    @Test
    @DisplayName("setSize updates snapshot dimensions")
    void shouldAcceptSizeChange() {
        ChartViewerApplication.setSize(1024, 768);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.width()).isEqualTo(1024);
        assertThat(snap.height()).isEqualTo(768);
    }

    @Test
    @DisplayName("addValues with step throws on more values than series (#865)")
    void shouldThrowOnMoreValuesThanSeries() {
        ChartViewerApplication.setSeries(List.of("OnlyOne"), List.of());

        assertThatThrownBy(() ->
                ChartViewerApplication.addValues(List.of(1.0, 2.0, 3.0), List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value count (3) does not match series count (1)");
    }

    @Test
    @DisplayName("addValues with step throws on fewer values than series (#865)")
    void shouldThrowOnFewerValuesThanSeries() {
        ChartViewerApplication.setSeries(List.of("A", "B", "C"), List.of());

        assertThatThrownBy(() ->
                ChartViewerApplication.addValues(List.of(1.0), List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value count (1) does not match series count (3)");
    }

    @Test
    @DisplayName("addValues with timestamp throws on more values than series (#865)")
    void shouldThrowOnMoreValuesThanSeriesWithTimestamp() {
        ChartViewerApplication.setSeries(List.of("OnlyOne"), List.of());

        assertThatThrownBy(() ->
                ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(),
                        LocalDateTime.of(2026, 1, 1, 12, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value count (2) does not match series count (1)");
    }

    @Test
    @DisplayName("addValues with timestamp throws on fewer values than series (#865)")
    void shouldThrowOnFewerValuesThanSeriesWithTimestamp() {
        ChartViewerApplication.setSeries(List.of("A", "B", "C"), List.of());

        assertThatThrownBy(() ->
                ChartViewerApplication.addValues(List.of(1.0), List.of(),
                        LocalDateTime.of(2026, 1, 1, 12, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value count (1) does not match series count (3)");
    }

    @Test
    @DisplayName("addValues throws on zero values when series exist (#865)")
    void shouldThrowOnZeroValuesWhenSeriesExist() {
        ChartViewerApplication.setSeries(List.of("A"), List.of());

        assertThatThrownBy(() ->
                ChartViewerApplication.addValues(List.of(), List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value count (0) does not match series count (1)");
    }

    @Test
    @DisplayName("addValues succeeds when values match series count exactly (#865)")
    void shouldSucceedWhenValuesMatchSeriesCount() {
        ChartViewerApplication.setSeries(List.of("A", "B"), List.of("C"));
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).hasSize(3);
        assertThat(snap.series().get(0).getData()).hasSize(1);
        assertThat(snap.series().get(1).getData()).hasSize(1);
        assertThat(snap.series().get(2).getData()).hasSize(1);
    }

    @Test
    @DisplayName("snapshot captures default formatter field (#1060)")
    void shouldCaptureDefaultFormatterInSnapshot() {
        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.formatter()).isEqualTo(DateTimeFormatter.BASIC_ISO_DATE);
    }

    @Test
    @DisplayName("reset clears all accumulated state")
    void shouldClearStateOnReset() {
        ChartViewerApplication.setSeries(List.of("A", "B"), List.of("C"));
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);
        ChartViewerApplication.setSize(1920, 1080);

        ChartViewerApplication.reset();

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).isEmpty();
        assertThat(snap.width()).isEqualTo(800);
        assertThat(snap.height()).isEqualTo(600);
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
    @DisplayName("reader thread sees size set by writer thread via synchronized snapshot")
    void shouldReadSizeUnderLockFromAnotherThread() throws InterruptedException {
        // Set size on this thread
        ChartViewerApplication.setSize(1024, 768);

        // Read it back from another thread to verify thread-safe publication
        AtomicReference<ChartViewerApplication.ChartData> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            // snapshot() reads under LOCK; the values set above should be visible
            result.set(ChartViewerApplication.snapshot());
            latch.countDown();
        });
        reader.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get().width()).isEqualTo(1024);
        assertThat(result.get().height()).isEqualTo(768);
    }
}
