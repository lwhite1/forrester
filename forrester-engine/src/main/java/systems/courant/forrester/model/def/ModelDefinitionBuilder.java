package systems.courant.forrester.model.def;

import systems.courant.forrester.model.ModelMetadata;

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
    private final List<LookupTableDef> lookupTables = new ArrayList<>();
    private final List<ModuleInstanceDef> modules = new ArrayList<>();
    private final List<SubscriptDef> subscripts = new ArrayList<>();
    private final List<CldVariableDef> cldVariables = new ArrayList<>();
    private final List<CausalLinkDef> causalLinks = new ArrayList<>();
    private final List<ViewDef> views = new ArrayList<>();
    private final List<ReferenceDataset> referenceDatasets = new ArrayList<>();
    private SimulationSettings defaultSimulation;
    private ModelMetadata metadata;

    /**
     * Sets the model name.
     *
     * @param name the model name (must not be blank)
     * @return this builder
     */
    public ModelDefinitionBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets an optional descriptive comment for the model.
     *
     * @param comment the comment text, or {@code null} for none
     * @return this builder
     */
    public ModelDefinitionBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the module interface, making this definition usable as a reusable module with
     * declared input and output ports.
     *
     * @param moduleInterface the module interface, or {@code null} if this is not a module
     * @return this builder
     */
    public ModelDefinitionBuilder moduleInterface(ModuleInterface moduleInterface) {
        this.moduleInterface = moduleInterface;
        return this;
    }

    /**
     * Adds a stock with the given name, initial value, and unit.
     *
     * @param name         the stock name
     * @param initialValue the initial numeric value
     * @param unit         the unit name
     * @return this builder
     */
    public ModelDefinitionBuilder stock(String name, double initialValue, String unit) {
        stocks.add(new StockDef(name, initialValue, unit));
        return this;
    }

    /**
     * Adds a stock with full details including comment and negative value policy.
     *
     * @param name                the stock name
     * @param comment             optional description
     * @param initialValue        the initial numeric value
     * @param unit                the unit name
     * @param negativeValuePolicy the policy name ({@code "CLAMP_TO_ZERO"}, {@code "ALLOW"},
     *                            or {@code "THROW"}), or {@code null} for default
     * @return this builder
     */
    public ModelDefinitionBuilder stock(String name, String comment, double initialValue,
                                        String unit, String negativeValuePolicy) {
        stocks.add(new StockDef(name, comment, initialValue, unit, negativeValuePolicy));
        return this;
    }

    /**
     * Adds a subscripted stock that will be expanded into scalar elements during compilation.
     *
     * @param name         the stock name
     * @param initialValue the initial numeric value
     * @param unit         the unit name
     * @param subscripts   the subscript dimension names this stock is over
     * @return this builder
     */
    public ModelDefinitionBuilder stock(String name, double initialValue, String unit,
                                        List<String> subscripts) {
        stocks.add(new StockDef(name, null, initialValue, unit, null, subscripts));
        return this;
    }

    /**
     * Adds a pre-built stock definition.
     *
     * @param stockDef the stock definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder stock(StockDef stockDef) {
        stocks.add(stockDef);
        return this;
    }

    /**
     * Adds a flow with the given parameters.
     *
     * @param name     the flow name
     * @param equation the formula expression string
     * @param timeUnit the time unit name for the flow rate
     * @param source   the source stock name, or {@code null} for an external source
     * @param sink     the sink stock name, or {@code null} for an external sink
     * @return this builder
     */
    public ModelDefinitionBuilder flow(String name, String equation, String timeUnit,
                                       String source, String sink) {
        flows.add(new FlowDef(name, equation, timeUnit, source, sink));
        return this;
    }

    /**
     * Adds a flow with an explicit material unit.
     *
     * @param name         the flow name
     * @param equation     the formula expression string
     * @param timeUnit     the time unit name for the flow rate
     * @param materialUnit the material unit name (e.g. "Person"), or null to infer from stock
     * @param source       the source stock name, or {@code null} for an external source
     * @param sink         the sink stock name, or {@code null} for an external sink
     * @return this builder
     */
    public ModelDefinitionBuilder flow(String name, String equation, String timeUnit,
                                       String materialUnit, String source, String sink) {
        flows.add(new FlowDef(name, null, equation, timeUnit, materialUnit,
                source, sink, List.of()));
        return this;
    }

    /**
     * Adds a subscripted flow that will be expanded into scalar elements during compilation.
     *
     * @param name       the flow name
     * @param equation   the formula expression string
     * @param timeUnit   the time unit name
     * @param source     the source stock name, or null
     * @param sink       the sink stock name, or null
     * @param subscripts the subscript dimension names
     * @return this builder
     */
    public ModelDefinitionBuilder flow(String name, String equation, String timeUnit,
                                       String source, String sink, List<String> subscripts) {
        flows.add(new FlowDef(name, null, equation, timeUnit, source, sink, subscripts));
        return this;
    }

    /**
     * Adds a pre-built flow definition.
     *
     * @param flowDef the flow definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder flow(FlowDef flowDef) {
        flows.add(flowDef);
        return this;
    }

    /**
     * Adds an auxiliary variable with the given name, equation, and unit.
     *
     * @param name     the auxiliary name
     * @param equation the formula expression string
     * @param unit     the unit name
     * @return this builder
     */
    public ModelDefinitionBuilder aux(String name, String equation, String unit) {
        auxiliaries.add(new AuxDef(name, equation, unit));
        return this;
    }

    /**
     * Adds a subscripted auxiliary that will be expanded into scalar elements during compilation.
     *
     * @param name       the auxiliary name
     * @param equation   the formula expression string
     * @param unit       the unit name
     * @param subscripts the subscript dimension names
     * @return this builder
     */
    public ModelDefinitionBuilder aux(String name, String equation, String unit,
                                      List<String> subscripts) {
        auxiliaries.add(new AuxDef(name, null, equation, unit, subscripts));
        return this;
    }

    /**
     * Adds a pre-built auxiliary definition.
     *
     * @param auxDef the auxiliary definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder aux(AuxDef auxDef) {
        auxiliaries.add(auxDef);
        return this;
    }

    /**
     * Adds a literal-valued auxiliary (parameter) with the given name, value, and unit.
     *
     * @param name  the parameter name
     * @param value the numeric value
     * @param unit  the unit name
     * @return this builder
     */
    public ModelDefinitionBuilder constant(String name, double value, String unit) {
        auxiliaries.add(new AuxDef(name, null, value, unit));
        return this;
    }

    /**
     * Adds a lookup table with the given data points and interpolation method.
     *
     * @param name          the table name (referenced in formulas as {@code LOOKUP(name, input)})
     * @param xValues       the x-axis data points (must be strictly increasing, at least 2)
     * @param yValues       the y-axis data points (same length as xValues)
     * @param interpolation the interpolation method: {@code "LINEAR"} or {@code "SPLINE"}
     * @return this builder
     */
    public ModelDefinitionBuilder lookupTable(String name, double[] xValues, double[] yValues,
                                              String interpolation) {
        lookupTables.add(new LookupTableDef(name, xValues, yValues, interpolation));
        return this;
    }

    /**
     * Adds a pre-built lookup table definition.
     *
     * @param tableDef the lookup table definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder lookupTable(LookupTableDef tableDef) {
        lookupTables.add(tableDef);
        return this;
    }

    /**
     * Adds a module instance with port bindings for composing nested models.
     *
     * @param instanceName   the unique instance name within this model
     * @param definition     the module's model definition
     * @param inputBindings  maps port name to expression string providing the value
     * @param outputBindings maps port name to alias name in the parent model
     * @return this builder
     */
    public ModelDefinitionBuilder module(String instanceName, ModelDefinition definition,
                                         Map<String, String> inputBindings,
                                         Map<String, String> outputBindings) {
        modules.add(new ModuleInstanceDef(instanceName, definition, inputBindings, outputBindings));
        return this;
    }

    /**
     * Adds a pre-built module instance definition.
     *
     * @param moduleDef the module instance definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder module(ModuleInstanceDef moduleDef) {
        modules.add(moduleDef);
        return this;
    }

    /**
     * Adds a subscript dimension with the given labels.
     *
     * @param name   the subscript name
     * @param labels the ordered list of labels (must contain at least one)
     * @return this builder
     */
    public ModelDefinitionBuilder subscript(String name, List<String> labels) {
        subscripts.add(new SubscriptDef(name, labels));
        return this;
    }

    /**
     * Adds a causal loop diagram variable with the given name and no comment.
     *
     * @param name the variable name
     * @return this builder
     */
    public ModelDefinitionBuilder cldVariable(String name) {
        cldVariables.add(new CldVariableDef(name));
        return this;
    }

    /**
     * Adds a causal loop diagram variable with the given name and comment.
     *
     * @param name    the variable name
     * @param comment optional description
     * @return this builder
     */
    public ModelDefinitionBuilder cldVariable(String name, String comment) {
        cldVariables.add(new CldVariableDef(name, comment));
        return this;
    }

    /**
     * Adds a pre-built CLD variable definition.
     *
     * @param def the CLD variable definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder cldVariable(CldVariableDef def) {
        cldVariables.add(def);
        return this;
    }

    /**
     * Adds a causal link between two variables with the specified polarity.
     *
     * @param from     the source variable name
     * @param to       the target variable name
     * @param polarity the direction of influence
     * @return this builder
     */
    public ModelDefinitionBuilder causalLink(String from, String to, CausalLinkDef.Polarity polarity) {
        causalLinks.add(new CausalLinkDef(from, to, polarity));
        return this;
    }

    /**
     * Adds a pre-built causal link definition.
     *
     * @param def the causal link definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder causalLink(CausalLinkDef def) {
        causalLinks.add(def);
        return this;
    }

    /**
     * Adds a graphical view definition describing element layout and connector routing.
     *
     * @param viewDef the view definition to add
     * @return this builder
     */
    public ModelDefinitionBuilder view(ViewDef viewDef) {
        views.add(viewDef);
        return this;
    }

    /**
     * Sets the default simulation settings for the model.
     *
     * @param timeStep     the time step unit name
     * @param duration     the simulation duration (must be positive and finite)
     * @param durationUnit the duration unit name
     * @return this builder
     */
    public ModelDefinitionBuilder defaultSimulation(String timeStep, double duration,
                                                     String durationUnit) {
        this.defaultSimulation = new SimulationSettings(timeStep, duration, durationUnit);
        return this;
    }

    /**
     * Sets the default simulation settings from a pre-built object.
     *
     * @param settings the simulation settings, or {@code null} for none
     * @return this builder
     */
    public ModelDefinitionBuilder defaultSimulation(SimulationSettings settings) {
        this.defaultSimulation = settings;
        return this;
    }

    /**
     * Sets optional attribution and licensing metadata.
     *
     * @param metadata the metadata, or {@code null} for none
     * @return this builder
     */
    public ModelDefinitionBuilder metadata(ModelMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /** Clears all stock definitions. */
    public ModelDefinitionBuilder clearStocks() { stocks.clear(); return this; }

    /** Clears all flow definitions. */
    public ModelDefinitionBuilder clearFlows() { flows.clear(); return this; }

    /** Clears all auxiliary definitions. */
    public ModelDefinitionBuilder clearAuxiliaries() { auxiliaries.clear(); return this; }

    /** Clears all view definitions. */
    public ModelDefinitionBuilder clearViews() { views.clear(); return this; }

    /** Adds all stock definitions from the given list. */
    public ModelDefinitionBuilder stocks(List<StockDef> defs) { stocks.addAll(defs); return this; }

    /** Adds all flow definitions from the given list. */
    public ModelDefinitionBuilder flows(List<FlowDef> defs) { flows.addAll(defs); return this; }

    /** Adds all auxiliary definitions from the given list. */
    public ModelDefinitionBuilder auxiliaries(List<AuxDef> defs) { auxiliaries.addAll(defs); return this; }

    /**
     * Adds a reference dataset for model validation.
     *
     * @param dataset the reference dataset to add
     * @return this builder
     */
    public ModelDefinitionBuilder referenceDataset(ReferenceDataset dataset) {
        referenceDatasets.add(dataset);
        return this;
    }

    /** Adds all reference datasets from the given list. */
    public ModelDefinitionBuilder referenceDatasets(List<ReferenceDataset> defs) { referenceDatasets.addAll(defs); return this; }

    /** Clears all reference datasets. */
    public ModelDefinitionBuilder clearReferenceDatasets() { referenceDatasets.clear(); return this; }

    /**
     * Builds and returns an immutable {@link ModelDefinition} from the accumulated state.
     * The model name must have been set via {@link #name(String)} before calling this method.
     *
     * <p>Example usage:
     * <pre>{@code
     * ModelDefinition def = new ModelDefinitionBuilder()
     *         .name("Population")
     *         .stock("Population", 1000, "people")
     *         .flow("births", "Population * birth_rate", "year", null, "Population")
     *         .constant("birth rate", 0.03, "1/year")
     *         .build();
     * }</pre>
     *
     * @return the constructed model definition
     */
    public ModelDefinition build() {
        return new ModelDefinition(
                name, comment, moduleInterface,
                stocks, flows, auxiliaries, lookupTables,
                modules, subscripts, cldVariables, causalLinks,
                views, defaultSimulation, metadata, referenceDatasets);
    }
}
