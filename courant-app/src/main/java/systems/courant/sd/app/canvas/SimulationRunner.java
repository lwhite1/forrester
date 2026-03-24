package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.SimulationSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @param stockNames  names of columns that represent stocks (as opposed to variables)
     */
    public record SimulationResult(List<String> columnNames, List<double[]> rows,
                                   Map<String, String> units, Set<String> stockNames) {

        public SimulationResult {
            columnNames = List.copyOf(columnNames);
            rows = rows.stream().map(double[]::clone).toList();
            units = Map.copyOf(units);
            stockNames = Set.copyOf(stockNames);
        }

        /** Constructor for callers that don't track stock names. */
        public SimulationResult(List<String> columnNames, List<double[]> rows,
                                Map<String, String> units) {
            this(columnNames, rows, units, Set.of());
        }

        /** Backwards-compatible constructor for tests and callers that don't have units. */
        public SimulationResult(List<String> columnNames, List<double[]> rows) {
            this(columnNames, rows, Map.of(), Set.of());
        }
    }

    /**
     * Compiles the model definition, runs a simulation with the given settings,
     * and returns captured time-series data. Uses the compiler's internal
     * {@link systems.courant.sd.measure.UnitRegistry} for time unit
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

        DataCaptureHandler handler = new DataCaptureHandler(settings.timeStep());
        sim.addEventHandler(handler);
        sim.execute();

        return new SimulationResult(handler.getColumnNames(), handler.getRows(),
                handler.getUnits(), handler.getStockNames());
    }

    /**
     * Event handler that captures column names at simulation start and
     * stock/variable values at each time step.
     */
    public static class DataCaptureHandler implements EventHandler {

        private final String timeStepLabel;
        private final List<String> columnNames = new ArrayList<>();
        private final List<double[]> rows = new ArrayList<>();
        private final Map<String, String> units = new LinkedHashMap<>();
        private final Set<String> stockNames = new LinkedHashSet<>();

        /** Snapshotted element references in column order, set at simulation start. */
        private List<Stock> capturedStocks;
        private List<Variable> capturedVariables;

        public DataCaptureHandler(String timeStepLabel) {
            this.timeStepLabel = timeStepLabel;
        }

        @Override
        public void handleSimulationStartEvent(SimulationStartEvent event) {
            Model model = event.getModel();
            columnNames.add(timeStepLabel);

            capturedStocks = new ArrayList<>(model.getStocks());
            for (Stock stock : capturedStocks) {
                columnNames.add(stock.getName());
                stockNames.add(stock.getName());
                units.put(stock.getName(),
                        stock.getUnit() != null ? stock.getUnit().getName() : "");
            }

            capturedVariables = new ArrayList<>(model.getVariables());
            for (Variable variable : capturedVariables) {
                columnNames.add(variable.getName());
                units.put(variable.getName(),
                        variable.getUnit() != null ? variable.getUnit().getName() : "");
            }
        }

        @Override
        public void handleTimeStepEvent(TimeStepEvent event) {
            long step = event.getStep();

            double[] row = new double[1 + capturedStocks.size() + capturedVariables.size()];
            row[0] = step;

            int idx = 1;
            for (Stock stock : capturedStocks) {
                row[idx++] = stock.getValue();
            }
            for (Variable variable : capturedVariables) {
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

        Set<String> getStockNames() {
            return stockNames;
        }
    }
}
