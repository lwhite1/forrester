package com.deathrayresearch.forrester.ui;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;


/**
 * Class that implements the {@link EventHandler} interface and controls the display of plots of stock levels.
 */
public class StockLevelChartViewer implements EventHandler {

    @Override
    public void handleTimeStepEvent(TimeStepEvent event) {
        ChartViewerApplication.addValues(
                event.getModel().getStockValues(),
                event.getModel().getVariableValues(),
                //event.getCurrentTime(),
                event.getStep());
    }

    @Override
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ChartViewerApplication.setSimulation(event.getSimulation());
        ChartViewerApplication.addSeries(event.getModel().getStockNames(), event.getModel().getVariableNames());
    }

    @Override
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        ChartViewerApplication.launch(ChartViewerApplication.class);
    }
}
