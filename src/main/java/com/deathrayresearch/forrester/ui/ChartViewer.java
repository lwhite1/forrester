package com.deathrayresearch.forrester.ui;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimestepEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Class that implements the {@link EventHandler} interface and controls the chart printing.
 */
public class ChartViewer implements EventHandler {

    @Override
    @Subscribe
    public void handleTimeStepEvent(TimestepEvent event) {
        ChartViewerApplication.addValues(event.getModel().getStockValues(),
                event.getModel().getVariableValues(), event.getCurrentTime());
    }

    @Override
    @Subscribe
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ChartViewerApplication.setSimulation(event.getSimulation());
        ChartViewerApplication.addSeries(event.getModel().getStockNames(), event.getModel().getVariableNames());
    }

    @Override
    @Subscribe
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        ChartViewerApplication.launch(ChartViewerApplication.class);
    }
}
