package com.deathrayresearch.forrester.model.def;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for constructing {@link ModelDefinition} instances programmatically.
 */
public class ModelDefinitionBuilder {

    private String name;
    private String comment;
    private ModuleInterface moduleInterface;
    private final List<StockDef> stocks = new ArrayList<>();
    private final List<FlowDef> flows = new ArrayList<>();
    private final List<AuxDef> auxiliaries = new ArrayList<>();
    private final List<ConstantDef> constants = new ArrayList<>();
    private final List<LookupTableDef> lookupTables = new ArrayList<>();
    private final List<ModuleInstanceDef> modules = new ArrayList<>();
    private final List<SubscriptDef> subscripts = new ArrayList<>();
    private final List<ViewDef> views = new ArrayList<>();
    private SimulationSettings defaultSimulation;

    public ModelDefinitionBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ModelDefinitionBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public ModelDefinitionBuilder moduleInterface(ModuleInterface moduleInterface) {
        this.moduleInterface = moduleInterface;
        return this;
    }

    public ModelDefinitionBuilder stock(String name, double initialValue, String unit) {
        stocks.add(new StockDef(name, initialValue, unit));
        return this;
    }

    public ModelDefinitionBuilder stock(String name, String comment, double initialValue,
                                        String unit, String negativeValuePolicy) {
        stocks.add(new StockDef(name, comment, initialValue, unit, negativeValuePolicy));
        return this;
    }

    public ModelDefinitionBuilder stock(StockDef stockDef) {
        stocks.add(stockDef);
        return this;
    }

    public ModelDefinitionBuilder flow(String name, String equation, String timeUnit,
                                       String source, String sink) {
        flows.add(new FlowDef(name, equation, timeUnit, source, sink));
        return this;
    }

    public ModelDefinitionBuilder flow(FlowDef flowDef) {
        flows.add(flowDef);
        return this;
    }

    public ModelDefinitionBuilder aux(String name, String equation, String unit) {
        auxiliaries.add(new AuxDef(name, equation, unit));
        return this;
    }

    public ModelDefinitionBuilder aux(AuxDef auxDef) {
        auxiliaries.add(auxDef);
        return this;
    }

    public ModelDefinitionBuilder constant(String name, double value, String unit) {
        constants.add(new ConstantDef(name, value, unit));
        return this;
    }

    public ModelDefinitionBuilder constant(ConstantDef constantDef) {
        constants.add(constantDef);
        return this;
    }

    public ModelDefinitionBuilder lookupTable(String name, double[] xValues, double[] yValues,
                                              String interpolation) {
        lookupTables.add(new LookupTableDef(name, xValues, yValues, interpolation));
        return this;
    }

    public ModelDefinitionBuilder lookupTable(LookupTableDef tableDef) {
        lookupTables.add(tableDef);
        return this;
    }

    public ModelDefinitionBuilder module(String instanceName, ModelDefinition definition,
                                         Map<String, String> inputBindings,
                                         Map<String, String> outputBindings) {
        modules.add(new ModuleInstanceDef(instanceName, definition, inputBindings, outputBindings));
        return this;
    }

    public ModelDefinitionBuilder module(ModuleInstanceDef moduleDef) {
        modules.add(moduleDef);
        return this;
    }

    public ModelDefinitionBuilder subscript(String name, List<String> labels) {
        subscripts.add(new SubscriptDef(name, labels));
        return this;
    }

    public ModelDefinitionBuilder view(ViewDef viewDef) {
        views.add(viewDef);
        return this;
    }

    public ModelDefinitionBuilder defaultSimulation(String timeStep, double duration,
                                                     String durationUnit) {
        this.defaultSimulation = new SimulationSettings(timeStep, duration, durationUnit);
        return this;
    }

    public ModelDefinition build() {
        return new ModelDefinition(
                name, comment, moduleInterface,
                stocks, flows, auxiliaries, constants, lookupTables,
                modules, subscripts, views, defaultSimulation);
    }
}
