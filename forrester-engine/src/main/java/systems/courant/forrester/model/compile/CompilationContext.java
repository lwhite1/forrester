package systems.courant.forrester.model.compile;

import systems.courant.forrester.measure.TimeUnit;
import systems.courant.forrester.measure.UnitRegistry;
import systems.courant.forrester.model.Constant;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.LookupTable;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.model.def.LookupTableDef;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Holds name→object mappings for a model being compiled. Supports parent contexts
 * for module scoping.
 */
public class CompilationContext {

    private final Map<String, Stock> stocks = new LinkedHashMap<>();
    private final Map<String, Flow> flows = new LinkedHashMap<>();
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final Map<String, Constant> constants = new LinkedHashMap<>();
    private final Map<String, LookupTable> lookupTables = new LinkedHashMap<>();
    private final Map<String, double[]> lookupInputHolders = new LinkedHashMap<>();
    private final Map<String, LookupTableDef> lookupTableDefs = new LinkedHashMap<>();

    private final CompilationContext parent;
    private final UnitRegistry unitRegistry;
    private final IntSupplier currentStep;
    private final double[] dtHolder;
    private final TimeUnit[] simTimeUnitHolder;

    /**
     * Creates a root compilation context with no parent and a default DT of 1.0.
     *
     * @param unitRegistry the unit registry for resolving unit names
     * @param currentStep  supplies the current simulation timestep
     */
    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep) {
        this(unitRegistry, currentStep, null, new double[]{1.0}, new TimeUnit[1]);
    }

    /**
     * Creates a child compilation context that inherits the parent's DT holder.
     *
     * @param unitRegistry the unit registry for resolving unit names
     * @param currentStep  supplies the current simulation timestep
     * @param parent       the parent context for scoped name resolution, or {@code null}
     */
    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep,
                              CompilationContext parent) {
        this(unitRegistry, currentStep, parent,
                parent != null ? parent.dtHolder : new double[]{1.0},
                parent != null ? parent.simTimeUnitHolder : new TimeUnit[1]);
    }

    /**
     * Creates a compilation context with an explicit parent and DT holder.
     *
     * @param unitRegistry the unit registry for resolving unit names
     * @param currentStep  supplies the current simulation timestep
     * @param parent       the parent context for scoped name resolution, or {@code null}
     * @param dtHolder     a single-element array holding the current DT value
     */
    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep,
                              CompilationContext parent, double[] dtHolder) {
        this(unitRegistry, currentStep, parent, dtHolder,
                parent != null ? parent.simTimeUnitHolder : new TimeUnit[1]);
    }

    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep,
                              CompilationContext parent, double[] dtHolder,
                              TimeUnit[] simTimeUnitHolder) {
        this.unitRegistry = unitRegistry;
        this.currentStep = currentStep;
        this.parent = parent;
        this.dtHolder = dtHolder;
        this.simTimeUnitHolder = simTimeUnitHolder;
    }

    /**
     * Registers a stock in this context.
     *
     * @param name  the stock name used for resolution
     * @param stock the stock instance
     */
    public void addStock(String name, Stock stock) {
        stocks.put(name, stock);
    }

    /**
     * Registers a flow in this context.
     *
     * @param name the flow name used for resolution
     * @param flow the flow instance
     */
    public void addFlow(String name, Flow flow) {
        flows.put(name, flow);
    }

    /**
     * Registers a variable in this context.
     *
     * @param name     the variable name used for resolution
     * @param variable the variable instance
     */
    public void addVariable(String name, Variable variable) {
        variables.put(name, variable);
    }

    /**
     * Registers a constant in this context.
     *
     * @param name     the constant name used for resolution
     * @param constant the constant instance
     */
    public void addConstant(String name, Constant constant) {
        constants.put(name, constant);
    }

    /**
     * Registers a lookup table and its mutable input holder in this context.
     *
     * @param name        the table name used for resolution
     * @param table       the lookup table instance
     * @param inputHolder a single-element array used to pass input values to the table
     */
    public void addLookupTable(String name, LookupTable table, double[] inputHolder) {
        lookupTables.put(name, table);
        lookupInputHolders.put(name, inputHolder);
    }

    /**
     * Registers a lookup table definition so fresh instances can be created on demand.
     *
     * @param name the table name
     * @param def  the table definition containing x/y data and interpolation mode
     */
    public void addLookupTableDef(String name, LookupTableDef def) {
        lookupTableDefs.put(name, def);
    }

    /**
     * Resolves a LookupTableDef by name so callers can create fresh LookupTable instances
     * with isolated input holders.
     */
    public Optional<LookupTableDef> resolveLookupTableDef(String name) {
        LookupTableDef def = lookupTableDefs.get(name);
        if (def != null) {
            return Optional.of(def);
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            def = lookupTableDefs.get(spaceName);
            if (def != null) {
                return Optional.of(def);
            }
        }
        if (parent != null) {
            return parent.resolveLookupTableDef(name);
        }
        return Optional.empty();
    }

    /**
     * Creates a fresh LookupTable from the stored definition with the given input supplier.
     * Each caller gets an isolated table with its own input, preventing cross-formula interference.
     */
    public Optional<LookupTable> createFreshLookupTable(String name, DoubleSupplier inputSupplier) {
        return resolveLookupTableDef(name).map(def -> {
            if ("SPLINE".equalsIgnoreCase(def.interpolation())) {
                return LookupTable.spline(def.xValues(), def.yValues(), inputSupplier);
            }
            return LookupTable.linear(def.xValues(), def.yValues(), inputSupplier);
        });
    }

    /**
     * Resolves a named value (stock, flow, variable, constant, or lookup table) to a double.
     * Checks local context first, then parent.
     *
     * @throws CompilationException if the name is not found
     */
    public double resolveValue(String name) {
        // Try exact match first
        OptionalDouble val = resolveValueLocal(name);
        if (val.isPresent()) {
            return val.getAsDouble();
        }
        // Try underscore→space fallback
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            val = resolveValueLocal(spaceName);
            if (val.isPresent()) {
                return val.getAsDouble();
            }
        }
        // Try parent
        if (parent != null) {
            return parent.resolveValue(name);
        }
        throw new CompilationException("Unresolved reference: " + name, name);
    }

    private OptionalDouble resolveValueLocal(String name) {
        Stock stock = stocks.get(name);
        if (stock != null) {
            return OptionalDouble.of(stock.getValue());
        }
        Constant constant = constants.get(name);
        if (constant != null) {
            return OptionalDouble.of(constant.getValue());
        }
        Variable variable = variables.get(name);
        if (variable != null) {
            return OptionalDouble.of(variable.getValue());
        }
        Flow flow = flows.get(name);
        if (flow != null) {
            TimeUnit resolveUnit = simTimeUnitHolder[0] != null
                    ? simTimeUnitHolder[0] : flow.getTimeUnit();
            return OptionalDouble.of(flow.flowPerTimeUnit(resolveUnit).getValue());
        }
        return OptionalDouble.empty();
    }

    /**
     * Resolves a constant value by name (for compile-time evaluation).
     * Returns null if not found as a constant.
     */
    public OptionalDouble resolveConstant(String name) {
        Constant constant = constants.get(name);
        if (constant != null) {
            return OptionalDouble.of(constant.getValue());
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            constant = constants.get(spaceName);
            if (constant != null) {
                return OptionalDouble.of(constant.getValue());
            }
        }
        if (parent != null) {
            return parent.resolveConstant(name);
        }
        return OptionalDouble.empty();
    }

    /**
     * Resolves a shared lookup table by name, trying underscore-to-space fallback
     * and parent context if not found locally.
     *
     * @param name the table name to look up
     * @return the lookup table, or empty if not found
     */
    public Optional<LookupTable> resolveLookupTable(String name) {
        LookupTable table = lookupTables.get(name);
        if (table != null) {
            return Optional.of(table);
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            table = lookupTables.get(spaceName);
            if (table != null) {
                return Optional.of(table);
            }
        }
        if (parent != null) {
            return parent.resolveLookupTable(name);
        }
        return Optional.empty();
    }

    /**
     * Returns an unmodifiable map of stocks registered in this context (local only).
     */
    public Map<String, Stock> getStocks() {
        return Collections.unmodifiableMap(stocks);
    }

    /**
     * Returns an unmodifiable map of flows registered in this context (local only).
     */
    public Map<String, Flow> getFlows() {
        return Collections.unmodifiableMap(flows);
    }

    /**
     * Returns an unmodifiable map of variables registered in this context (local only).
     */
    public Map<String, Variable> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Returns an unmodifiable map of constants registered in this context (local only).
     */
    public Map<String, Constant> getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    /**
     * Resolves the mutable input holder for a lookup table by name.
     * Tries underscore-to-space fallback and parent context if not found locally.
     *
     * @param name the table name
     * @return the single-element input holder array, or empty if not found
     */
    public Optional<double[]> resolveLookupInputHolder(String name) {
        double[] holder = lookupInputHolders.get(name);
        if (holder != null) {
            return Optional.of(holder);
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            holder = lookupInputHolders.get(spaceName);
            if (holder != null) {
                return Optional.of(holder);
            }
        }
        if (parent != null) {
            return parent.resolveLookupInputHolder(name);
        }
        return Optional.empty();
    }

    /**
     * Returns the unit registry used for resolving unit names during compilation.
     */
    public UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    /**
     * Returns the supplier for the current simulation timestep.
     */
    public IntSupplier getCurrentStep() {
        return currentStep;
    }

    /**
     * Returns the current DT (integration time step) value.
     */
    public double getDt() {
        return dtHolder[0];
    }

    /**
     * Returns the mutable single-element array holding the DT value.
     * Used to share the DT value across compiled formulas.
     */
    public double[] getDtHolder() {
        return dtHolder;
    }

    /**
     * Returns the mutable single-element array holding the simulation time unit.
     * Used to resolve flow values in the correct time unit during simulation.
     */
    public TimeUnit[] getSimTimeUnitHolder() {
        return simTimeUnitHolder;
    }
}
