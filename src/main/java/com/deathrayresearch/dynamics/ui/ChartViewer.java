package com.deathrayresearch.dynamics.ui;

import com.deathrayresearch.dynamics.event.EventHandler;
import com.deathrayresearch.dynamics.event.SimulationEndEvent;
import com.deathrayresearch.dynamics.event.SimulationStartEvent;
import com.deathrayresearch.dynamics.event.TimestepEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Class that implements the {@link EventHandler} interface and controls the chart printing.
 */
public class ChartViewer implements EventHandler {

    public static ChartViewer newInstance(EventBus eventBus) {
        ChartViewer subscriber = new ChartViewer();
        eventBus.register(subscriber);
        return subscriber;
    }

    @Override
    @Subscribe
    public void handleTimestepEvent(TimestepEvent event) {
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
