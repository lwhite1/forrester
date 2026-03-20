package systems.courant.sd.model.compile;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.TimeUnit;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.LookupTable;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.NegativeValuePolicy;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.TimeSeries;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.def.TimeSeriesDef;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.DefinitionValidator;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.PortDef;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;
import systems.courant.sd.model.graph.DependencyGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * Compiles a {@link ModelDefinition} into a runnable {@link Model}.
 *
 * <p>Uses a two-pass approach with mutable formula holders:
 * <ol>
 *     <li>Pass 1: Create all elements with DoubleSupplier[] indirection holders</li>
 *     <li>Pass 2: Compile formulas and fill in the holders</li>
 * </ol>
 *
 * <p>The indirection pattern allows forward references: a flow can reference a variable
 * that hasn't been compiled yet, because both read through holders that are filled
 * after all elements are registered.
 */
public class ModelCompiler {

    private static final Logger log = LoggerFactory.getLogger(ModelCompiler.class);

    private final UnitRegistry unitRegistry;

    /**
     * Creates a model compiler with a default {@link UnitRegistry}.
     */
    public ModelCompiler() {
        this(new UnitRegistry());
    }

    /**
     * Creates a model compiler with the given unit registry.
     *
     * @param unitRegistry the unit registry for resolving unit names in definitions
     */
    public ModelCompiler(UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }

    /**
     * Compiles a model definition into a runnable compiled model.
     *
     * @param def the model definition to compile
     * @return a compiled model ready for simulation
     * @throws CompilationException if any element references cannot be resolved
     */
    public CompiledModel compile(ModelDefinition def) {
        List<String> errors = DefinitionValidator.validateStructure(def);
        if (!errors.isEmpty()) {
            throw new CompilationException(
                    "Model validation failed: " + String.join("; ", errors),
                    def.name() != null ? def.name() : "");
        }

        // Pre-expand subscripted elements into scalar elements
        ModelDefinition expandedDef = SubscriptExpander.expand(def);

        // Detect algebraic loops (cycles among variables) and warn.
        // Cycles are handled at runtime by Variable's re-entrancy guard, which
        // returns the previous timestep's value to break the loop.
        // Runs on the expanded definition so cross-dimensional dependencies are visible.
        DependencyGraph depGraph = DependencyGraph.fromDefinition(expandedDef);
        List<Set<String>> sccs = depGraph.findSCCs();
        for (Set<String> scc : sccs) {
            log.warn("Algebraic loop detected (will use previous-step values to converge): {}",
                    scc);
        }

        Model model = new Model(expandedDef.name());
        if (def.metadata() != null) {
            model.setMetadata(def.metadata());
        }
        List<Resettable> resettables = new ArrayList<>();
        long[] stepHolder = {0};
        double[] dtHolder = {1.0};
        TimeUnit[] simTimeUnitHolder = new TimeUnit[1];

        CompilationContext context = new CompilationContext(
                unitRegistry, () -> stepHolder[0], null, dtHolder, simTimeUnitHolder);

        compileInto(expandedDef, model, context, resettables, stepHolder);

        return new CompiledModel(model, resettables, def, stepHolder, dtHolder,
                simTimeUnitHolder, unitRegistry, context.getWarnings());
    }

    /**
     * Compiles a definition's elements into the given model and context.
     */
    private void compileInto(ModelDefinition def, Model model,
                             CompilationContext context,
                             List<Resettable> resettables, long[] stepHolder) {
        // Inject simulation-derived constants (TIME_STEP, INITIAL_TIME, FINAL_TIME)
        // so formulas can reference them without requiring explicit variable definitions.
        injectSimulationConstants(def, context);

        // Stocks — create all first, then evaluate initial expressions
        // (expressions may reference other stocks)
        for (StockDef sDef : def.stocks()) {
            Stock stock = createStock(sDef, context);
            model.addStock(stock);
            context.addStock(sDef.name(), stock);
        }
        resolveInitialExpressions(def.stocks(), context);

        buildLookupTables(def, context);
        buildTimeSeries(def, model, context, stepHolder);

        // Variables — use DoubleSupplier[] holders for indirection
        List<DoubleSupplier[]> auxHolders = new ArrayList<>();
        for (VariableDef aDef : def.variables()) {
            if (aDef.isLiteral()) {
                context.addLiteralConstant(aDef.name(), aDef.literalValue());
            }
            DoubleSupplier[] holder = createAuxHolder();
            auxHolders.add(holder);
            Variable variable = new Variable(aDef.name(), unitRegistry.resolve(aDef.unit()),
                    () -> holder[0].getAsDouble());
            model.addVariable(variable);
            context.addVariable(aDef.name(), variable);
        }

        // Flows — use DoubleSupplier[] holders for indirection
        List<DoubleSupplier[]> flowHolders = new ArrayList<>();
        for (FlowDef fDef : def.flows()) {
            DoubleSupplier[] holder = {() -> 0};
            flowHolders.add(holder);
            Flow flow = createFlow(fDef, holder, context);
            model.addFlow(flow);
            context.addFlow(fDef.name(), flow);
            wireFlowSourceSink(fDef, flow, context, "");
        }

        // Module instances
        for (ModuleInstanceDef mDef : def.modules()) {
            compileModule(mDef, model, context, resettables, stepHolder);
        }

        compileFormulas(def, context, resettables, auxHolders, flowHolders);
    }

    private void compileModule(ModuleInstanceDef mDef, Model parentModel,
                               CompilationContext parentContext,
                               List<Resettable> resettables, long[] stepHolder) {
        ModelDefinition innerDef = mDef.definition();
        Module module = new Module(mDef.instanceName());
        String errorContext = " in module '" + mDef.instanceName() + "'";

        CompilationContext moduleContext = new CompilationContext(
                unitRegistry, () -> stepHolder[0], parentContext);

        // Input bindings (compiled in parent context)
        for (Map.Entry<String, String> binding : mDef.inputBindings().entrySet()) {
            String portName = binding.getKey();
            String expression = binding.getValue();
            ExprCompiler parentCompiler = new ExprCompiler(parentContext, resettables);
            DoubleSupplier supplier = parentCompiler.compileExpr(
                    ExprParser.parse(expression));
            Unit bindingUnit = resolvePortUnit(portName, mDef);
            Variable bindingVar = new Variable(portName, bindingUnit,
                    supplier::getAsDouble);
            moduleContext.addVariable(portName, bindingVar);
        }

        // Stocks — create all first, then evaluate initial expressions
        for (StockDef sDef : innerDef.stocks()) {
            Stock stock = createStock(sDef, moduleContext);
            module.addStock(stock);
            moduleContext.addStock(sDef.name(), stock);
        }
        resolveInitialExpressions(innerDef.stocks(), moduleContext);

        buildLookupTables(innerDef, moduleContext);
        buildTimeSeries(innerDef, module, moduleContext, stepHolder);

        // Variables with holder indirection
        List<DoubleSupplier[]> auxHolders = new ArrayList<>();
        for (VariableDef aDef : innerDef.variables()) {
            if (aDef.isLiteral()) {
                moduleContext.addLiteralConstant(aDef.name(), aDef.literalValue());
            }
            DoubleSupplier[] holder = createAuxHolder();
            auxHolders.add(holder);
            Variable variable = new Variable(aDef.name(), unitRegistry.resolve(aDef.unit()),
                    () -> holder[0].getAsDouble());
            module.addVariable(variable);
            moduleContext.addVariable(aDef.name(), variable);
        }

        // Flows with holder indirection
        List<DoubleSupplier[]> flowHolders = new ArrayList<>();
        for (FlowDef fDef : innerDef.flows()) {
            DoubleSupplier[] holder = {() -> 0};
            flowHolders.add(holder);
            Flow flow = createFlow(fDef, holder, moduleContext);
            module.addFlow(flow);
            moduleContext.addFlow(fDef.name(), flow);
            wireFlowSourceSink(fDef, flow, moduleContext, errorContext);
        }

        compileFormulas(innerDef, moduleContext, resettables, auxHolders, flowHolders);

        // Output bindings
        for (Map.Entry<String, String> binding : mDef.outputBindings().entrySet()) {
            String portName = binding.getKey();
            String alias = binding.getValue();
            Variable moduleVar = moduleContext.getVariables().get(portName);
            if (moduleVar == null) {
                throw new CompilationException(
                        "Module '" + mDef.instanceName()
                                + "' output binding references unknown port: " + portName,
                        portName);
            }
            Variable aliasVar = new Variable(alias, moduleVar.getUnit(),
                    moduleVar::getValue);
            parentContext.addVariable(alias, aliasVar);
            parentModel.addVariable(aliasVar);
        }

        parentModel.addModule(module);
    }

    private void injectSimulationConstants(ModelDefinition def, CompilationContext context) {
        SimulationSettings sim = def.defaultSimulation();
        if (sim == null) {
            return;
        }
        context.addLiteralConstant("TIME_STEP", sim.dt());
        context.addLiteralConstant("INITIAL_TIME", sim.initialTime());
        context.addLiteralConstant("FINAL_TIME", sim.initialTime() + sim.duration());
    }

    // === Shared helpers ===

    private Stock createStock(StockDef sDef, CompilationContext context) {
        Unit unit = unitRegistry.resolve(sDef.unit());
        NegativeValuePolicy policy = resolvePolicy(sDef.negativeValuePolicy());
        return new Stock(sDef.name(), sDef.initialValue(), unit, policy);
    }

    /**
     * Evaluates initial expressions for stocks that have them.
     * Called after all stocks are created and registered in the context,
     * so expressions can reference other stocks.
     */
    private void resolveInitialExpressions(List<StockDef> stocks, CompilationContext context) {
        for (StockDef sDef : stocks) {
            if (sDef.initialExpression() == null) {
                continue;
            }
            try {
                ExprCompiler exprCompiler = new ExprCompiler(context, new ArrayList<>());
                DoubleSupplier supplier = exprCompiler.compileExpr(
                        ExprParser.parse(sDef.initialExpression()));
                double initVal = supplier.getAsDouble();
                Stock stock = context.getStocks().get(sDef.name());
                if (stock != null) {
                    stock.setValue(initVal);
                }
            } catch (ParseException | CompilationException | ArithmeticException e) {
                log.warn("Stock '{}': failed to evaluate initialExpression '{}', "
                                + "falling back to numeric initialValue ({}). Reason: {}",
                        sDef.name(), sDef.initialExpression(), sDef.initialValue(), e.toString());
            }
        }
    }

    private void buildLookupTables(ModelDefinition def, CompilationContext context) {
        for (LookupTableDef tDef : def.lookupTables()) {
            double[] inputHolder = {0};
            LookupTable table;
            if ("SPLINE".equalsIgnoreCase(tDef.interpolation())) {
                table = LookupTable.spline(tDef.xValues(), tDef.yValues(),
                        () -> inputHolder[0]);
            } else {
                table = LookupTable.linear(tDef.xValues(), tDef.yValues(),
                        () -> inputHolder[0]);
            }
            context.addLookupTable(tDef.name(), table, inputHolder);
            context.addLookupTableDef(tDef.name(), tDef);
        }
    }

    private void buildTimeSeries(ModelDefinition def, Model model,
                                  CompilationContext context, long[] stepHolder) {
        buildTimeSeriesInto(def, model::addVariable, context, stepHolder);
    }

    private void buildTimeSeries(ModelDefinition def, Module module,
                                  CompilationContext context, long[] stepHolder) {
        buildTimeSeriesInto(def, module::addVariable, context, stepHolder);
    }

    private void buildTimeSeriesInto(ModelDefinition def,
                                      java.util.function.Consumer<Variable> variableAdder,
                                      CompilationContext context, long[] stepHolder) {
        for (TimeSeriesDef tsDef : def.timeSeries()) {
            double[] dtHolder = context.getDtHolder();
            DoubleSupplier timeSupplier = () -> stepHolder[0] * dtHolder[0];
            TimeSeries ts = TimeSeries.create(
                    tsDef.timeValues(), tsDef.dataValues(),
                    timeSupplier, tsDef.interpolation(), tsDef.extrapolation());
            Unit unit = tsDef.unit() != null ? unitRegistry.resolve(tsDef.unit())
                    : unitRegistry.resolve("Thing");
            Variable variable = new Variable(tsDef.name(), unit, ts::getCurrentValue);
            variableAdder.accept(variable);
            context.addVariable(tsDef.name(), variable);
        }
    }

    private DoubleSupplier[] createAuxHolder() {
        return new DoubleSupplier[]{() -> 0};
    }

    private Flow createFlow(FlowDef fDef, DoubleSupplier[] holder,
                            CompilationContext context) {
        TimeUnit timeUnit = unitRegistry.resolveTimeUnit(fDef.timeUnit());
        Unit flowUnit = fDef.materialUnit() != null
                ? unitRegistry.resolve(fDef.materialUnit())
                : resolveFlowUnit(fDef, context);
        return Flow.create(fDef.name(), timeUnit, flowUnit,
                () -> new Quantity(holder[0].getAsDouble(), flowUnit));
    }

    private void wireFlowSourceSink(FlowDef fDef, Flow flow,
                                    CompilationContext context, String errorContext) {
        if (fDef.source() != null) {
            Stock source = context.getStocks().get(fDef.source());
            if (source == null) {
                throw new CompilationException(
                        "Flow '" + fDef.name() + "'" + errorContext
                                + " references unknown source: " + fDef.source(),
                        fDef.name());
            }
            source.addOutflow(flow);
        }
        if (fDef.sink() != null) {
            Stock sink = context.getStocks().get(fDef.sink());
            if (sink == null) {
                throw new CompilationException(
                        "Flow '" + fDef.name() + "'" + errorContext
                                + " references unknown sink: " + fDef.sink(),
                        fDef.name());
            }
            sink.addInflow(flow);
        }
    }

    private void compileFormulas(ModelDefinition def, CompilationContext context,
                                 List<Resettable> resettables,
                                 List<DoubleSupplier[]> auxHolders,
                                 List<DoubleSupplier[]> flowHolders) {
        ExprCompiler exprCompiler = new ExprCompiler(context, resettables);
        for (int i = 0; i < def.variables().size(); i++) {
            VariableDef aDef = def.variables().get(i);
            DoubleSupplier compiled = exprCompiler.compileExpr(
                    ExprParser.parse(aDef.equation()));
            auxHolders.get(i)[0] = compiled;
        }
        for (int i = 0; i < def.flows().size(); i++) {
            FlowDef fDef = def.flows().get(i);
            DoubleSupplier compiled = exprCompiler.compileExpr(
                    ExprParser.parse(fDef.equation()));
            flowHolders.get(i)[0] = compiled;
        }
    }

    private NegativeValuePolicy resolvePolicy(String policyName) {
        if (policyName == null) {
            return NegativeValuePolicy.CLAMP_TO_ZERO;
        }
        try {
            return NegativeValuePolicy.valueOf(policyName);
        } catch (IllegalArgumentException e) {
            throw new CompilationException(
                    "Unknown NegativeValuePolicy: " + policyName, policyName);
        }
    }

    private Unit resolvePortUnit(String portName, ModuleInstanceDef mDef) {
        if (mDef.definition().moduleInterface() != null) {
            for (PortDef port : mDef.definition().moduleInterface().inputs()) {
                if (port.name().equals(portName) && port.unit() != null) {
                    return unitRegistry.resolve(port.unit());
                }
            }
        }
        return unitRegistry.resolve("Thing");
    }

    private Unit resolveFlowUnit(FlowDef fDef, CompilationContext context) {
        if (fDef.sink() != null) {
            Stock sink = context.getStocks().get(fDef.sink());
            if (sink != null) {
                return sink.getUnit();
            }
        }
        if (fDef.source() != null) {
            Stock source = context.getStocks().get(fDef.source());
            if (source != null) {
                return source.getUnit();
            }
        }
        return unitRegistry.resolve("Thing");
    }
}
