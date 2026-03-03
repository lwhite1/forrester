package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.compile.CompiledModel;
import com.deathrayresearch.forrester.model.compile.ModelCompiler;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.SimulationSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Compiles a {@link ModelDefinition}, runs a simulation with the given settings,
 * and captures time-series data for display.
 */
public class SimulationRunner {

    /**
     * Immutable result of a simulation run containing column names and row data.
     */
    public record SimulationResult(List<String> columnNames, List<double[]> rows) {

        public SimulationResult {
            columnNames = List.copyOf(columnNames);
            rows = List.copyOf(rows);
        }
    }

    /**
     * Compiles the model definition, runs a simulation with the given settings,
     * and returns captured time-series data.
     *
     * @param def the model definition to compile and simulate
     * @param settings the simulation settings (time step, duration, duration unit)
     * @return the captured simulation results
     */
    public SimulationResult run(ModelDefinition def, SimulationSettings settings) {
        ModelCompiler compiler = new ModelCompiler();
        CompiledModel compiled = compiler.compile(def);

        UnitRegistry registry = new UnitRegistry();
        TimeUnit timeStep = registry.resolveTimeUnit(settings.timeStep());
        TimeUnit durationUnit = registry.resolveTimeUnit(settings.durationUnit());

        Simulation sim = compiled.createSimulation(timeStep, settings.duration(), durationUnit);

        DataCaptureHandler handler = new DataCaptureHandler();
        sim.addEventHandler(handler);
        sim.execute();

        return new SimulationResult(handler.columnNames, handler.rows);
    }

    /**
     * Event handler that captures column names at simulation start and
     * stock/variable values at each time step.
     */
    static class DataCaptureHandler implements EventHandler {

        final List<String> columnNames = new ArrayList<>();
        final List<double[]> rows = new ArrayList<>();

        @Override
        public void handleSimulationStartEvent(SimulationStartEvent event) {
            Model model = event.getModel();
            columnNames.add("Step");
            for (Stock stock : model.getStocks()) {
                columnNames.add(stock.getName());
            }
            Collection<Variable> variables = model.getVariables();
            for (Variable variable : variables) {
                columnNames.add(variable.getName());
            }
        }

        @Override
        public void handleTimeStepEvent(TimeStepEvent event) {
            Model model = event.getModel();
            int step = event.getStep();

            List<Stock> stocks = model.getStocks();
            Collection<Variable> variables = model.getVariables();
            double[] row = new double[1 + stocks.size() + variables.size()];

            row[0] = step;
            int idx = 1;
            for (Stock stock : stocks) {
                row[idx++] = stock.getValue();
            }
            for (Variable variable : variables) {
                row[idx++] = variable.getValue();
            }
            rows.add(row);
        }
    }
}
