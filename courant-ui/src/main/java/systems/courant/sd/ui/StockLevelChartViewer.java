package systems.courant.sd.ui;

import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.SimulationEndEvent;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;


/**
 * Class that implements the {@link EventHandler} interface and controls the display of plots of stock levels.
 */
public class StockLevelChartViewer implements EventHandler {

    @Override
    public void handleTimeStepEvent(TimeStepEvent event) {
        ChartViewerApplication.addValues(
                event.getModel().getStockValues(),
                event.getModel().getVariableValues(),
                event.getStep());
    }

    @Override
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ChartViewerApplication.setSimulation(event.getSimulation());
        ChartViewerApplication.setSeries(event.getModel().getStockNames(), event.getModel().getVariableNames());
    }

    @Override
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        ChartViewerApplication.showChart();
    }
}
