package systems.courant.sd.ui;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.THING;

/**
 * Tests for StockLevelChartViewer event handling.
 * Uses ApplicationExtension to initialize the JavaFX toolkit (needed for chart series).
 */
@DisplayName("StockLevelChartViewer")
@ExtendWith(ApplicationExtension.class)
class StockLevelChartViewerTest {

    private StockLevelChartViewer viewer;
    private Model model;
    private Simulation simulation;

    @BeforeEach
    void setUp() {
        ChartViewerApplication.reset();
        model = new Model("Growth Model");
        Stock population = new Stock("Population", 100, THING);
        Flow births = Flow.create("Births", MINUTE,
                () -> new Quantity(population.getValue() * 0.05, THING));
        population.addInflow(births);
        model.addStock(population);
        model.addFlow(births);

        simulation = new Simulation(model, MINUTE, MINUTE, 5);
        viewer = new StockLevelChartViewer();
    }

    @Test
    @DisplayName("handleSimulationStartEvent initializes series from model")
    void shouldInitializeSeriesFromModel() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).hasSize(1);
        assertThat(snap.series().getFirst().getName()).isEqualTo("Population");
    }

    @Test
    @DisplayName("handleTimeStepEvent records stock value")
    void shouldRecordStockValue() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        TimeStepEvent event = new TimeStepEvent(
                LocalDateTime.now(), model, 0, MINUTE);
        viewer.handleTimeStepEvent(event);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series().getFirst().getData()).hasSize(1);
        assertThat(snap.series().getFirst().getData().getFirst().getYValue().doubleValue())
                .isEqualTo(100.0);
    }

    @Test
    @DisplayName("Multiple time steps accumulate stock values")
    void shouldAccumulateMultipleStockValues() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        for (int i = 0; i < 5; i++) {
            TimeStepEvent event = new TimeStepEvent(
                    LocalDateTime.now(), model, i, MINUTE);
            viewer.handleTimeStepEvent(event);
        }

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series().getFirst().getData()).hasSize(5);
    }
}
