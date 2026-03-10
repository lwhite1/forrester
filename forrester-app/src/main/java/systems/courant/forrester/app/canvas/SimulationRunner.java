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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link ModelDefinition}, runs a simulation with the given settings,
 * and captures time-series data for display.
 */
public class SimulationRunner {

    /**
     * Immutable result of a simulation run containing column names and row data.
     * Row arrays are defensively copied to ensure true immutability.
     */
    /**
     * @param columnNames ordered column names (Step, stocks, variables)
     * @param rows        time-series data, one array per step
     * @param units       element name to unit string (empty string if no unit)
     */
    public record SimulationResult(List<String> columnNames, List<double[]> rows,
                                   Map<String, String> units) {

        public SimulationResult {
            columnNames = List.copyOf(columnNames);
            rows = rows.stream().map(double[]::clone).toList();
            units = Map.copyOf(units);
        }

        /** Backwards-compatible constructor for tests and callers that don't have units. */
        public SimulationResult(List<String> columnNames, List<double[]> rows) {
            this(columnNames, rows, Map.of());
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
        ModelDefinition defWithSettings = def.toBuilder()
                .defaultSimulation(settings)
                .build();

        ModelCompiler compiler = new ModelCompiler();
        CompiledModel compiled = compiler.compile(defWithSettings);
        Simulation sim = compiled.createSimulation();

        DataCaptureHandler handler = new DataCaptureHandler();
        sim.addEventHandler(handler);
        sim.execute();

        return new SimulationResult(handler.getColumnNames(), handler.getRows(),
                handler.getUnits());
    }

    /**
     * Event handler that captures column names at simulation start and
     * stock/variable values at each time step.
     */
    static class DataCaptureHandler implements EventHandler {

        private final List<String> columnNames = new ArrayList<>();
        private final List<double[]> rows = new ArrayList<>();
        private final Map<String, String> units = new LinkedHashMap<>();

        @Override
        public void handleSimulationStartEvent(SimulationStartEvent event) {
            Model model = event.getModel();
            columnNames.add("Step");
            for (Stock stock : model.getStocks()) {
                columnNames.add(stock.getName());
                units.put(stock.getName(),
                        stock.getUnit() != null ? stock.getUnit().getName() : "");
            }
            Collection<Variable> variables = model.getVariables();
            for (Variable variable : variables) {
                columnNames.add(variable.getName());
                units.put(variable.getName(),
                        variable.getUnit() != null ? variable.getUnit().getName() : "");
            }
        }

        @Override
        public void handleTimeStepEvent(TimeStepEvent event) {
            Model model = event.getModel();
            long step = event.getStep();

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

        Map<String, String> getUnits() {
            return units;
        }
    }
}
