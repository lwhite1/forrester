package systems.courant.sd.ui;

import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.SimulationEndEvent;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.model.Flow;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An {@link EventHandler} that controls the display of plots of flow rates over time.
 */
public class FlowChartViewer implements EventHandler {

    private final Flow[] flows;

    public FlowChartViewer(Flow... flows) {
        this.flows = flows;
    }

    @Override
    public void handleTimeStepEvent(TimeStepEvent event) {
        ArrayList<Double> flowValues = new ArrayList<>();
        for (Flow flow : flows) {
            flowValues.add(flow.flowPerTimeUnit(event.getTimeStep()).getValue());
        }
        ChartViewerApplication.addValues(
                new ArrayList<>(),
                flowValues,
                event.getStep());
    }

    @Override
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ChartViewerApplication.setSimulation(event.getSimulation());
        ChartViewerApplication.addFlowSeries(Arrays.stream(flows).map(Flow::getName).collect(Collectors.toList()));
    }

    @Override
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        ChartViewerApplication.showChart();
    }
}
