package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.graph.ConnectorGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable model editing layer that sits between the UI and the engine's immutable
 * {@link ModelDefinition}. Supports adding and removing elements while preserving
 * the engine's immutability contract — an immutable snapshot can be rebuilt on demand
 * via {@link #toModelDefinition()}.
 */
public class ModelEditor {

    private String modelName = "Untitled";
    private final List<StockDef> stocks = new ArrayList<>();
    private final List<FlowDef> flows = new ArrayList<>();
    private final List<AuxDef> auxiliaries = new ArrayList<>();
    private final List<ConstantDef> constants = new ArrayList<>();
    private final List<LookupTableDef> lookupTables = new ArrayList<>();
    private int nextId = 1;

    /**
     * Loads all elements from an immutable {@link ModelDefinition} into mutable lists,
     * clearing any previous state.
     */
    public void loadFrom(ModelDefinition definition) {
        modelName = definition.name();
        stocks.clear();
        flows.clear();
        auxiliaries.clear();
        constants.clear();
        lookupTables.clear();

        stocks.addAll(definition.stocks());
        flows.addAll(definition.flows());
        auxiliaries.addAll(definition.auxiliaries());
        constants.addAll(definition.constants());
        lookupTables.addAll(definition.lookupTables());

        // Set nextId past any existing numeric suffix
        nextId = 1;
        updateNextId(stocks.stream().map(StockDef::name));
        updateNextId(flows.stream().map(FlowDef::name));
        updateNextId(auxiliaries.stream().map(AuxDef::name));
        updateNextId(constants.stream().map(ConstantDef::name));
    }

    private void updateNextId(java.util.stream.Stream<String> names) {
        names.forEach(name -> {
            // Try to extract trailing number from auto-generated names like "Stock 3"
            int spaceIdx = name.lastIndexOf(' ');
            if (spaceIdx >= 0) {
                try {
                    int num = Integer.parseInt(name.substring(spaceIdx + 1));
                    if (num >= nextId) {
                        nextId = num + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // Not an auto-named element
                }
            }
        });
    }

    /**
     * Adds a new stock with an auto-generated name.
     * @return the name of the created stock
     */
    public String addStock() {
        String name = "Stock " + nextId++;
        stocks.add(new StockDef(name, 0, "units"));
        return name;
    }

    /**
     * Adds a new flow with an auto-generated name and cloud-to-cloud connections.
     * @return the name of the created flow
     */
    public String addFlow() {
        String name = "Flow " + nextId++;
        flows.add(new FlowDef(name, "0", "day", null, null));
        return name;
    }

    /**
     * Adds a new auxiliary with an auto-generated name.
     * @return the name of the created auxiliary
     */
    public String addAux() {
        String name = "Aux " + nextId++;
        auxiliaries.add(new AuxDef(name, "0", "units"));
        return name;
    }

    /**
     * Adds a new constant with an auto-generated name.
     * @return the name of the created constant
     */
    public String addConstant() {
        String name = "Constant " + nextId++;
        constants.add(new ConstantDef(name, 0, "units"));
        return name;
    }

    /**
     * Removes the element with the given name from the appropriate list.
     * If a stock is removed, any flow referencing it as source or sink has
     * that connection nullified (becomes a cloud).
     */
    public void removeElement(String name) {
        boolean wasStock = stocks.removeIf(s -> s.name().equals(name));

        if (wasStock) {
            // Nullify flow source/sink references to the deleted stock
            for (int i = 0; i < flows.size(); i++) {
                FlowDef f = flows.get(i);
                boolean sourceMatch = name.equals(f.source());
                boolean sinkMatch = name.equals(f.sink());
                if (sourceMatch || sinkMatch) {
                    flows.set(i, new FlowDef(
                            f.name(),
                            f.comment(),
                            f.equation(),
                            f.timeUnit(),
                            sourceMatch ? null : f.source(),
                            sinkMatch ? null : f.sink()
                    ));
                }
            }
            return;
        }

        if (flows.removeIf(f -> f.name().equals(name))) {
            return;
        }
        if (auxiliaries.removeIf(a -> a.name().equals(name))) {
            return;
        }
        constants.removeIf(c -> c.name().equals(name));
    }

    public String getModelName() {
        return modelName;
    }

    public List<StockDef> getStocks() {
        return stocks;
    }

    public List<FlowDef> getFlows() {
        return flows;
    }

    public List<AuxDef> getAuxiliaries() {
        return auxiliaries;
    }

    public List<ConstantDef> getConstants() {
        return constants;
    }

    public List<LookupTableDef> getLookupTables() {
        return lookupTables;
    }

    /**
     * Rebuilds an immutable {@link ModelDefinition} snapshot from the current editor state.
     */
    public ModelDefinition toModelDefinition() {
        return new ModelDefinition(
                modelName,
                null,
                null,
                List.copyOf(stocks),
                List.copyOf(flows),
                List.copyOf(auxiliaries),
                List.copyOf(constants),
                List.copyOf(lookupTables),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    /**
     * Generates connector routes from the current model state's dependency graph.
     */
    public List<ConnectorRoute> generateConnectors() {
        return ConnectorGenerator.generate(toModelDefinition());
    }
}
