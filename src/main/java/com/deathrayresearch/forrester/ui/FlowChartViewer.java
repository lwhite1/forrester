package com.deathrayresearch.forrester.ui;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.model.Flow;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Class that implements the {@link EventHandler} interface and controls the display of plots of stock levels.
 */
public class FlowChartViewer implements EventHandler {

    private final Flow[] flows;

    public FlowChartViewer(Flow ... flows) {
        this.flows = flows;
    }

    @Override
    @Subscribe
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
    @Subscribe
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ChartViewerApplication.setSimulation(event.getSimulation());
        ChartViewerApplication.addFlowSeries(Arrays.stream(flows).map(Flow::getName).collect(Collectors.toList()));
    }

    @Override
    @Subscribe
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        ChartViewerApplication.launch(ChartViewerApplication.class);
    }
}
