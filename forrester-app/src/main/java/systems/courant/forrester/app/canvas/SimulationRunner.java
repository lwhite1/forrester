package systems.courant.forrester.app.canvas;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.event.EventHandler;
import systems.courant.forrester.event.SimulationStartEvent;
import systems.courant.forrester.event.TimeStepEvent;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.model.compile.CompiledModel;
import systems.courant.forrester.model.compile.ModelCompiler;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.SimulationSettings;

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
     * {@link systems.courant.forrester.measure.UnitRegistry} for time unit
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
