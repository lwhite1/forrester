package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.NegativeValuePolicy;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.expr.ExprParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private final UnitRegistry unitRegistry;

    public ModelCompiler() {
        this(new UnitRegistry());
    }

    public ModelCompiler(UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }

    /**
     * Compiles a model definition into a runnable compiled model.
     */
    public CompiledModel compile(ModelDefinition def) {
        Model model = new Model(def.name());
        List<Resettable> resettables = new ArrayList<>();
        int[] stepHolder = {0};

        CompilationContext context = new CompilationContext(unitRegistry, () -> stepHolder[0]);

        compileInto(def, model, context, resettables, stepHolder);

        return new CompiledModel(model, resettables, def, stepHolder, unitRegistry);
    }

    /**
     * Compiles a definition's elements into the given model and context.
     */
    private void compileInto(ModelDefinition def, Model model,
                             CompilationContext context,
                             List<Resettable> resettables, int[] stepHolder) {
        // === Pass 1: Constants, stocks, lookup tables, aux placeholders, flow placeholders ===

        // Constants
        for (ConstantDef cDef : def.constants()) {
            Unit unit = unitRegistry.resolve(cDef.unit());
            Constant constant = new Constant(cDef.name(), unit, cDef.value());
            model.addConstant(constant);
            context.addConstant(cDef.name(), constant);
        }

        // Stocks
        for (StockDef sDef : def.stocks()) {
            Unit unit = unitRegistry.resolve(sDef.unit());
            NegativeValuePolicy policy = resolvePolicy(sDef.negativeValuePolicy());
            Stock stock = new Stock(sDef.name(), sDef.initialValue(), unit, policy);
            model.addStock(stock);
            context.addStock(sDef.name(), stock);
        }

        // Lookup tables — use input holders that get wired when LOOKUP() is compiled
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
        }

        // Auxiliaries — use DoubleSupplier[] holders for indirection
        List<DoubleSupplier[]> auxHolders = new ArrayList<>();
        for (AuxDef aDef : def.auxiliaries()) {
            Unit unit = unitRegistry.resolve(aDef.unit());
            DoubleSupplier[] holder = {() -> 0};
            auxHolders.add(holder);
            Variable variable = new Variable(aDef.name(), unit,
                    () -> holder[0].getAsDouble());
            model.addVariable(variable);
            context.addVariable(aDef.name(), variable);
        }

        // Flows — use DoubleSupplier[] holders for indirection
        List<DoubleSupplier[]> flowHolders = new ArrayList<>();
        for (FlowDef fDef : def.flows()) {
            TimeUnit timeUnit = unitRegistry.resolveTimeUnit(fDef.timeUnit());
            Unit flowUnit = resolveFlowUnit(fDef, context);
            DoubleSupplier[] holder = {() -> 0};
            flowHolders.add(holder);
            Flow flow = Flow.create(fDef.name(), timeUnit,
                    () -> new Quantity(holder[0].getAsDouble(), flowUnit));
            context.addFlow(fDef.name(), flow);

            // Wire source/sink
            if (fDef.source() != null) {
                Stock source = context.getStocks().get(fDef.source());
                if (source == null) {
                    throw new CompilationException(
                            "Flow '" + fDef.name() + "' references unknown source: "
                                    + fDef.source(), fDef.name());
                }
                source.addOutflow(flow);
            }
            if (fDef.sink() != null) {
                Stock sink = context.getStocks().get(fDef.sink());
                if (sink == null) {
                    throw new CompilationException(
                            "Flow '" + fDef.name() + "' references unknown sink: "
                                    + fDef.sink(), fDef.name());
                }
                sink.addInflow(flow);
            }
        }

        // Module instances
        for (ModuleInstanceDef mDef : def.modules()) {
            compileModule(mDef, model, context, resettables, stepHolder);
        }

        // === Pass 2: Compile formulas and fill holders ===

        ExprCompiler exprCompiler = new ExprCompiler(context, resettables);

        for (int i = 0; i < def.auxiliaries().size(); i++) {
            AuxDef aDef = def.auxiliaries().get(i);
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

    private void compileModule(ModuleInstanceDef mDef, Model parentModel,
                               CompilationContext parentContext,
                               List<Resettable> resettables, int[] stepHolder) {
        ModelDefinition innerDef = mDef.definition();
        Module module = new Module(mDef.instanceName());

        CompilationContext moduleContext = new CompilationContext(
                unitRegistry, () -> stepHolder[0], parentContext);

        // Constants
        for (ConstantDef cDef : innerDef.constants()) {
            Unit unit = unitRegistry.resolve(cDef.unit());
            Constant constant = new Constant(cDef.name(), unit, cDef.value());
            moduleContext.addConstant(cDef.name(), constant);
        }

        // Input bindings (compiled in parent context)
        for (Map.Entry<String, String> binding : mDef.inputBindings().entrySet()) {
            String portName = binding.getKey();
            String expression = binding.getValue();
            ExprCompiler parentCompiler = new ExprCompiler(parentContext, resettables);
            DoubleSupplier supplier = parentCompiler.compileExpr(
                    ExprParser.parse(expression));
            Unit bindingUnit = unitRegistry.resolve("Thing");
            Variable bindingVar = new Variable(portName, bindingUnit,
                    supplier::getAsDouble);
            moduleContext.addVariable(portName, bindingVar);
        }

        // Stocks
        for (StockDef sDef : innerDef.stocks()) {
            Unit unit = unitRegistry.resolve(sDef.unit());
            NegativeValuePolicy policy = resolvePolicy(sDef.negativeValuePolicy());
            Stock stock = new Stock(sDef.name(), sDef.initialValue(), unit, policy);
            module.addStock(stock);
            moduleContext.addStock(sDef.name(), stock);
        }

        // Lookup tables
        for (LookupTableDef tDef : innerDef.lookupTables()) {
            double[] inputHolder = {0};
            LookupTable table;
            if ("SPLINE".equalsIgnoreCase(tDef.interpolation())) {
                table = LookupTable.spline(tDef.xValues(), tDef.yValues(),
                        () -> inputHolder[0]);
            } else {
                table = LookupTable.linear(tDef.xValues(), tDef.yValues(),
                        () -> inputHolder[0]);
            }
            moduleContext.addLookupTable(tDef.name(), table, inputHolder);
        }

        // Auxiliaries with holder indirection
        List<DoubleSupplier[]> auxHolders = new ArrayList<>();
        for (AuxDef aDef : innerDef.auxiliaries()) {
            Unit unit = unitRegistry.resolve(aDef.unit());
            DoubleSupplier[] holder = {() -> 0};
            auxHolders.add(holder);
            Variable variable = new Variable(aDef.name(), unit,
                    () -> holder[0].getAsDouble());
            module.addVariable(variable);
            moduleContext.addVariable(aDef.name(), variable);
        }

        // Flows with holder indirection
        List<DoubleSupplier[]> flowHolders = new ArrayList<>();
        for (FlowDef fDef : innerDef.flows()) {
            TimeUnit timeUnit = unitRegistry.resolveTimeUnit(fDef.timeUnit());
            Unit flowUnit = resolveFlowUnit(fDef, moduleContext);
            DoubleSupplier[] holder = {() -> 0};
            flowHolders.add(holder);
            Flow flow = Flow.create(fDef.name(), timeUnit,
                    () -> new Quantity(holder[0].getAsDouble(), flowUnit));
            module.addFlow(flow);
            moduleContext.addFlow(fDef.name(), flow);

            if (fDef.source() != null) {
                Stock source = moduleContext.getStocks().get(fDef.source());
                if (source == null) {
                    throw new CompilationException(
                            "Flow '" + fDef.name() + "' in module '"
                                    + mDef.instanceName()
                                    + "' references unknown source: "
                                    + fDef.source(), fDef.name());
                }
                source.addOutflow(flow);
            }
            if (fDef.sink() != null) {
                Stock sink = moduleContext.getStocks().get(fDef.sink());
                if (sink == null) {
                    throw new CompilationException(
                            "Flow '" + fDef.name() + "' in module '"
                                    + mDef.instanceName()
                                    + "' references unknown sink: "
                                    + fDef.sink(), fDef.name());
                }
                sink.addInflow(flow);
            }
        }

        // Compile formulas
        ExprCompiler moduleCompiler = new ExprCompiler(moduleContext, resettables);

        for (int i = 0; i < innerDef.auxiliaries().size(); i++) {
            AuxDef aDef = innerDef.auxiliaries().get(i);
            DoubleSupplier compiled = moduleCompiler.compileExpr(
                    ExprParser.parse(aDef.equation()));
            auxHolders.get(i)[0] = compiled;
        }

        for (int i = 0; i < innerDef.flows().size(); i++) {
            FlowDef fDef = innerDef.flows().get(i);
            DoubleSupplier compiled = moduleCompiler.compileExpr(
                    ExprParser.parse(fDef.equation()));
            flowHolders.get(i)[0] = compiled;
        }

        // Output bindings
        for (Map.Entry<String, String> binding : mDef.outputBindings().entrySet()) {
            String portName = binding.getKey();
            String alias = binding.getValue();
            Variable moduleVar = moduleContext.getVariables().get(portName);
            if (moduleVar != null) {
                Variable aliasVar = new Variable(alias, moduleVar.getUnit(),
                        moduleVar::getValue);
                parentContext.addVariable(alias, aliasVar);
                parentModel.addVariable(aliasVar);
            }
        }

        parentModel.addModule(module);
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
