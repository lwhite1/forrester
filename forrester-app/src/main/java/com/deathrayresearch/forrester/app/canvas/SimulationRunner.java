package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.event.EventHandler;
import com.deathrayresearch.forrester.event.SimulationStartEvent;
import com.deathrayresearch.forrester.event.TimeStepEvent;
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
     * Row arrays are defensively copied to ensure true immutability.
     */
    public record SimulationResult(List<String> columnNames, List<double[]> rows) {

        public SimulationResult {
            columnNames = List.copyOf(columnNames);
            rows = rows.stream().map(double[]::clone).toList();
        }
    }

    /**
     * Compiles the model definition, runs a simulation with the given settings,
     * and returns captured time-series data. Uses the compiler's internal
     * {@link com.deathrayresearch.forrester.measure.UnitRegistry} for time unit
     * resolution by embedding the settings in the definition before compilation.
     *
     * @param def the model definition to compile and simulate
     * @param settings the simulation settings (time step, duration, duration unit)
     * @return the captured simulation results
     */
    public SimulationResult run(ModelDefinition def, SimulationSettings settings) {
        ModelDefinition defWithSettings = new ModelDefinition(
                def.name(), def.comment(), def.moduleInterface(),
                def.stocks(), def.flows(), def.auxiliaries(), def.constants(),
                def.lookupTables(), def.modules(), def.subscripts(), def.views(),
                settings
        );

        ModelCompiler compiler = new ModelCompiler();
        CompiledModel compiled = compiler.compile(defWithSettings);
        Simulation sim = compiled.createSimulation();

        DataCaptureHandler handler = new DataCaptureHandler();
        sim.addEventHandler(handler);
        sim.execute();

        return new SimulationResult(handler.getColumnNames(), handler.getRows());
    }

    /**
     * Event handler that captures column names at simulation start and
     * stock/variable values at each time step.
     */
    static class DataCaptureHandler implements EventHandler {

        private final List<String> columnNames = new ArrayList<>();
        private final List<double[]> rows = new ArrayList<>();

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

        List<String> getColumnNames() {
            return columnNames;
        }

        List<double[]> getRows() {
            return rows;
        }
    }
}
