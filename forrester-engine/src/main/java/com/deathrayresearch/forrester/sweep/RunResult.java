package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationEndEvent;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.model.Model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link EventHandler} that captures stock and variable values at each time step
 * during a simulation run. Used by {@link ParameterSweep} to collect results for
 * a single parameter value.
 */
public class RunResult implements EventHandler {

    private final double parameterValue;
    private final Map<String, Double> parameterMap;

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
        this.parameterMap = Collections.emptyMap();
    }

    /**
     * Creates a new run result for a multi-parameter combination.
     *
     * @param parameterMap the parameter name-value pairs used for this run
     */
    public RunResult(Map<String, Double> parameterMap) {
        this.parameterMap = new LinkedHashMap<>(parameterMap);
        // Derive single parameterValue from first entry for backward compatibility
        this.parameterValue = parameterMap.isEmpty() ? 0.0
                : parameterMap.values().iterator().next();
    }

    @Override
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        Model model = event.getModel();
        stockNames = new ArrayList<>(model.getStockNames());
        variableNames = new ArrayList<>(model.getVariableNames());
    }

    @Override
    public void handleTimeStepEvent(TimeStepEvent event) {
        Model model = event.getModel();
        steps.add(event.getStep());
        stockSnapshots.add(new ArrayList<>(model.getStockValues()));
        variableSnapshots.add(new ArrayList<>(model.getVariableValues()));
    }

    @Override
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        // no-op
    }

    /**
     * Returns the single parameter value for this run, or the first parameter value
     * if this run was created with a multi-parameter map.
     *
     * @return the parameter value
     */
    public double getParameterValue() {
        return parameterValue;
    }

    /**
     * Returns the full parameter map for this run, or an empty map if the single-parameter
     * constructor was used.
     *
     * @return an unmodifiable map of parameter name to value
     */
    public Map<String, Double> getParameterMap() {
        return Collections.unmodifiableMap(parameterMap);
    }

    /**
     * Returns the names of the stocks recorded during this run.
     *
     * @return an unmodifiable list of stock names
     */
    public List<String> getStockNames() {
        return Collections.unmodifiableList(stockNames);
    }

    /**
     * Returns the names of the variables recorded during this run.
     *
     * @return an unmodifiable list of variable names
     */
    public List<String> getVariableNames() {
        return Collections.unmodifiableList(variableNames);
    }

    /**
     * Returns the number of time steps recorded during this run.
     *
     * @return the step count
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * Returns the step number at the given index in the recording sequence.
     *
     * @param index the zero-based index into the recorded steps
     * @return the step number
     */
    public int getStep(int index) {
        return steps.get(index);
    }

    /**
     * Returns the stock values captured at the given step index.
     *
     * @param index the zero-based index into the recorded steps
     * @return an unmodifiable list of stock values, in the same order as {@link #getStockNames()}
     */
    public List<Double> getStockValuesAtStep(int index) {
        return Collections.unmodifiableList(stockSnapshots.get(index));
    }

    /**
     * Returns the variable values captured at the given step index.
     *
     * @param index the zero-based index into the recorded steps
     * @return an unmodifiable list of variable values, in the same order as
     *         {@link #getVariableNames()}
     */
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
        if (stockSnapshots.isEmpty()) {
            throw new IllegalStateException("No steps have been recorded yet");
        }
        return stockSnapshots.get(stockSnapshots.size() - 1).get(stockIndex);
    }

    /**
     * Returns the full time series of values for the stock with the given name.
     *
     * @param stockName the stock name
     * @return an array of stock values, one per recorded step
     */
    public double[] getStockSeries(String stockName) {
        int stockIndex = stockNames.indexOf(stockName);
        if (stockIndex < 0) {
            throw new IllegalArgumentException("Unknown stock: " + stockName);
        }
        double[] series = new double[stockSnapshots.size()];
        for (int i = 0; i < stockSnapshots.size(); i++) {
            series[i] = stockSnapshots.get(i).get(stockIndex);
        }
        return series;
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
