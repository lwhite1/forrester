package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.model.Model;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An {@link EventHandler} that captures stock and variable values at each time step
 * during a simulation run. Used by {@link ParameterSweep} to collect results for
 * a single parameter value.
 */
public class RunResult implements EventHandler {

    private final double parameterValue;

    private List<String> stockNames = Collections.emptyList();
    private List<String> variableNames = Collections.emptyList();

    private final List<Integer> steps = new ArrayList<>();
    private final List<List<Double>> stockSnapshots = new ArrayList<>();
    private final List<List<Double>> variableSnapshots = new ArrayList<>();

    /**
     * Creates a new run result for the given parameter value.
     *
     * @param parameterValue the parameter value used for this run
     */
    public RunResult(double parameterValue) {
        this.parameterValue = parameterValue;
    }

    @Override
    @Subscribe
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        Model model = event.getModel();
        stockNames = new ArrayList<>(model.getStockNames());
        variableNames = new ArrayList<>(model.getVariableNames());
    }

    @Override
    @Subscribe
    public void handleTimeStepEvent(TimeStepEvent event) {
        Model model = event.getModel();
        steps.add(event.getStep());
        stockSnapshots.add(new ArrayList<>(model.getStockValues()));
        variableSnapshots.add(new ArrayList<>(model.getVariableValues()));
    }

    @Override
    @Subscribe
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        // no-op
    }

    public double getParameterValue() {
        return parameterValue;
    }

    public List<String> getStockNames() {
        return Collections.unmodifiableList(stockNames);
    }

    public List<String> getVariableNames() {
        return Collections.unmodifiableList(variableNames);
    }

    public int getStepCount() {
        return steps.size();
    }

    public int getStep(int index) {
        return steps.get(index);
    }

    public List<Double> getStockValuesAtStep(int index) {
        return Collections.unmodifiableList(stockSnapshots.get(index));
    }

    public List<Double> getVariableValuesAtStep(int index) {
        return Collections.unmodifiableList(variableSnapshots.get(index));
    }

    /**
     * Returns the final value of the stock with the given name.
     *
     * @param stockName the stock name
     * @return the stock value at the last recorded step
     */
    public double getFinalStockValue(String stockName) {
        int stockIndex = stockNames.indexOf(stockName);
        if (stockIndex < 0) {
            throw new IllegalArgumentException("Unknown stock: " + stockName);
        }
        return stockSnapshots.get(stockSnapshots.size() - 1).get(stockIndex);
    }

    /**
     * Returns the maximum value of the stock with the given name across all recorded steps.
     *
     * @param stockName the stock name
     * @return the peak stock value
     */
    public double getMaxStockValue(String stockName) {
        int stockIndex = stockNames.indexOf(stockName);
        if (stockIndex < 0) {
            throw new IllegalArgumentException("Unknown stock: " + stockName);
        }
        double max = Double.NEGATIVE_INFINITY;
        for (List<Double> snapshot : stockSnapshots) {
            double val = snapshot.get(stockIndex);
            if (val > max) {
                max = val;
            }
        }
        return max;
    }
}
