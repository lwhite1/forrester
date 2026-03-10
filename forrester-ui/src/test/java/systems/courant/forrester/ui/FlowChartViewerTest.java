package systems.courant.forrester.ui;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.event.SimulationStartEvent;
import systems.courant.forrester.event.TimeStepEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static systems.courant.forrester.measure.Units.MINUTE;
import static systems.courant.forrester.measure.Units.THING;

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
    @DisplayName("handleSimulationStartEvent initializes flow series without error")
    void shouldHandleStartEvent() {
        assertThatNoException().isThrownBy(() ->
                viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation)));
    }

    @Test
    @DisplayName("handleTimeStepEvent records flow values without error")
    void shouldHandleTimeStepEvent() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        TimeStepEvent event = new TimeStepEvent(
                LocalDateTime.now(), model, 0, MINUTE);

        assertThatNoException().isThrownBy(() ->
                viewer.handleTimeStepEvent(event));
    }

    @Test
    @DisplayName("Multiple time steps accumulate without error")
    void shouldHandleMultipleTimeSteps() {
        viewer.handleSimulationStartEvent(new SimulationStartEvent(simulation));

        for (int i = 0; i < 5; i++) {
            TimeStepEvent event = new TimeStepEvent(
                    LocalDateTime.now(), model, i, MINUTE);
            viewer.handleTimeStepEvent(event);
        }
    }
}
