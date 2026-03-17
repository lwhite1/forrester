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
 * Tests for FlowChartViewer event handling.
 * Uses ApplicationExtension to initialize the JavaFX toolkit (needed for chart series).
 */
@DisplayName("FlowChartViewer")
@ExtendWith(ApplicationExtension.class)
class FlowChartViewerTest {

    private FlowChartViewer viewer;
    private Flow flow;
    private Model model;
    private Simulation simulation;

    @BeforeEach
    void setUp() {
        ChartViewerApplication.reset();
        model = new Model("Test");
        Stock tank = new Stock("Tank", 100, THING);
        flow = Flow.create("Drain", MINUTE, () -> new Quantity(5.0, THING));
        tank.addOutflow(flow);
        model.addStock(tank);
        model.addFlow(flow);

        simulation = new Simulation(model, MINUTE, MINUTE, 3);
        viewer = new FlowChartViewer(flow);
    }

    @Test
    @DisplayName("handleSimulationStartEvent initializes flow series")
    void shouldInitializeFlowSeries() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series()).hasSize(1);
        assertThat(snap.series().getFirst().getName()).isEqualTo("Drain");
    }

    @Test
    @DisplayName("handleTimeStepEvent records flow value")
    void shouldRecordFlowValue() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        TimeStepEvent event = new TimeStepEvent(
                LocalDateTime.now(), model, 0, MINUTE);
        viewer.handleTimeStepEvent(event);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();
        assertThat(snap.series().getFirst().getData()).hasSize(1);
        assertThat(snap.series().getFirst().getData().getFirst().getYValue().doubleValue())
                .isEqualTo(5.0);
    }

    @Test
    @DisplayName("Multiple time steps accumulate data points")
    void shouldAccumulateMultipleDataPoints() {
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
