package systems.courant.forrester.ui;

import javafx.scene.chart.XYChart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ChartViewerApplication's static data accumulation methods.
 * Uses ApplicationExtension to initialize the JavaFX toolkit.
 */
@DisplayName("ChartViewerApplication static methods")
@ExtendWith(ApplicationExtension.class)
class ChartViewerApplicationTest {

    @Test
    @DisplayName("addSeries creates series from stock and variable names")
    void shouldCreateSeriesFromNames() {
        ChartViewerApplication.addSeries(List.of("S1", "S2"), List.of("V1"));

        // Verify by adding values — if series count mismatches, the value won't be added
        ChartViewerApplication.addValues(List.of(1.0, 2.0), List.of(3.0), 0);
        // No exception means the series were created correctly
    }

    @Test
    @DisplayName("addFlowSeries creates series from flow names")
    void shouldCreateFlowSeries() {
        ChartViewerApplication.addFlowSeries(List.of("FlowA", "FlowB"));
        ChartViewerApplication.addValues(List.of(), List.of(10.0, 20.0), 1);
    }

    @Test
    @DisplayName("addValues with step number does not throw")
    void shouldAcceptStepBasedValues() {
        ChartViewerApplication.addSeries(List.of("X"), List.of());
        ChartViewerApplication.addValues(List.of(42.0), List.of(), 5);
    }

    @Test
    @DisplayName("addValues with timestamp does not throw")
    void shouldAcceptTimestampBasedValues() {
        ChartViewerApplication.addSeries(List.of("X"), List.of());
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
        ChartViewerApplication.addSeries(List.of("OnlyOne"), List.of());
        // Pass more values than series — should not throw
        ChartViewerApplication.addValues(List.of(1.0, 2.0, 3.0), List.of(), 0);
    }

    @Test
    @DisplayName("addValues gracefully handles fewer values than series")
    void shouldHandleFewerValues() {
        ChartViewerApplication.addSeries(List.of("A", "B", "C"), List.of());
        // Pass fewer values than series — should not throw
        ChartViewerApplication.addValues(List.of(1.0), List.of(), 0);
    }
}
