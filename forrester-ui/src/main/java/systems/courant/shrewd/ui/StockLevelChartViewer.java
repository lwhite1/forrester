package systems.courant.shrewd.ui;

import systems.courant.shrewd.event.EventHandler;
import systems.courant.shrewd.event.SimulationEndEvent;
import systems.courant.shrewd.event.SimulationStartEvent;
import systems.courant.shrewd.event.TimeStepEvent;


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
        ChartViewerApplication.showChart();
    }
}
