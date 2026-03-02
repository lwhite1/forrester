package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import java.util.LinkedHashMap;
import java.util.Map;
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

    private final CompilationContext parent;
    private final UnitRegistry unitRegistry;
    private final IntSupplier currentStep;
    private final double dt;

    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep) {
        this(unitRegistry, currentStep, null, 1.0);
    }

    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep,
                              CompilationContext parent) {
        this(unitRegistry, currentStep, parent, 1.0);
    }

    public CompilationContext(UnitRegistry unitRegistry, IntSupplier currentStep,
                              CompilationContext parent, double dt) {
        this.unitRegistry = unitRegistry;
        this.currentStep = currentStep;
        this.parent = parent;
        this.dt = dt;
    }

    public void addStock(String name, Stock stock) {
        stocks.put(name, stock);
    }

    public void addFlow(String name, Flow flow) {
        flows.put(name, flow);
    }

    public void addVariable(String name, Variable variable) {
        variables.put(name, variable);
    }

    public void addConstant(String name, Constant constant) {
        constants.put(name, constant);
    }

    public void addLookupTable(String name, LookupTable table, double[] inputHolder) {
        lookupTables.put(name, table);
        lookupInputHolders.put(name, inputHolder);
    }

    /**
     * Resolves a named value (stock, flow, variable, constant, or lookup table) to a double.
     * Checks local context first, then parent.
     *
     * @throws CompilationException if the name is not found
     */
    public double resolveValue(String name) {
        // Try exact match first
        Double val = resolveValueLocal(name);
        if (val != null) {
            return val;
        }
        // Try underscore→space fallback
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            val = resolveValueLocal(spaceName);
            if (val != null) {
                return val;
            }
        }
        // Try parent
        if (parent != null) {
            return parent.resolveValue(name);
        }
        throw new CompilationException("Unresolved reference: " + name, name);
    }

    private Double resolveValueLocal(String name) {
        Stock stock = stocks.get(name);
        if (stock != null) {
            return stock.getValue();
        }
        Constant constant = constants.get(name);
        if (constant != null) {
            return constant.getValue();
        }
        Variable variable = variables.get(name);
        if (variable != null) {
            return variable.getValue();
        }
        Flow flow = flows.get(name);
        if (flow != null) {
            return flow.flowPerTimeUnit(flow.getTimeUnit()).getValue();
        }
        return null;
    }

    /**
     * Resolves a constant value by name (for compile-time evaluation).
     * Returns null if not found as a constant.
     */
    public Double resolveConstant(String name) {
        Constant constant = constants.get(name);
        if (constant != null) {
            return constant.getValue();
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            constant = constants.get(spaceName);
            if (constant != null) {
                return constant.getValue();
            }
        }
        if (parent != null) {
            return parent.resolveConstant(name);
        }
        return null;
    }

    public LookupTable resolveLookupTable(String name) {
        LookupTable table = lookupTables.get(name);
        if (table != null) {
            return table;
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            table = lookupTables.get(spaceName);
            if (table != null) {
                return table;
            }
        }
        if (parent != null) {
            return parent.resolveLookupTable(name);
        }
        return null;
    }

    public Map<String, Stock> getStocks() {
        return stocks;
    }

    public Map<String, Flow> getFlows() {
        return flows;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public Map<String, Constant> getConstants() {
        return constants;
    }

    public double[] resolveLookupInputHolder(String name) {
        double[] holder = lookupInputHolders.get(name);
        if (holder != null) {
            return holder;
        }
        if (name.contains("_")) {
            String spaceName = name.replace('_', ' ');
            holder = lookupInputHolders.get(spaceName);
            if (holder != null) {
                return holder;
            }
        }
        if (parent != null) {
            return parent.resolveLookupInputHolder(name);
        }
        return null;
    }

    public UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    public IntSupplier getCurrentStep() {
        return currentStep;
    }

    public double getDt() {
        return dt;
    }
}
