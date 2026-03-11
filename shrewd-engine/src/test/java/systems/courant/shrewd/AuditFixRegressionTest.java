package systems.courant.shrewd;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.UnitRegistry;
import systems.courant.shrewd.measure.units.item.ItemUnits;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Module;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.model.compile.CompiledModel;
import systems.courant.shrewd.model.compile.CompilationContext;
import systems.courant.shrewd.model.compile.ExprCompiler;
import systems.courant.shrewd.model.compile.ModelCompiler;
import systems.courant.shrewd.model.compile.Resettable;
import systems.courant.shrewd.model.def.DefinitionValidator;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.ModuleInterface;
import systems.courant.shrewd.model.def.PortDef;
import systems.courant.shrewd.sweep.Objectives;
import systems.courant.shrewd.sweep.OptimizationAlgorithm;
import systems.courant.shrewd.sweep.OptimizationResult;
import systems.courant.shrewd.sweep.Optimizer;
import systems.courant.shrewd.sweep.RunResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.THING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Regression tests for the 8 medium-severity audit fixes.
 */
@DisplayName("Audit fix regressions")
class AuditFixRegressionTest {

    // -----------------------------------------------------------------------
    // Fix 1: Optimizer bestRun null in fallback path
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 1: Optimizer bestRun null guard")
    class OptimizerBestRunFix {

        @Test
        void shouldReturnNonNullRunResultEvenWhenNoImprovement() {
            // Objective always returns MAX_VALUE so bestParams stays null,
            // but bestRun should still be captured from the first evaluation.
            OptimizationResult result = Optimizer.builder()
                    .parameter("rate", 0.01, 1.0)
                    .modelFactory(params -> buildSimpleModel(params.get("rate")))
                    .objective(run -> Double.MAX_VALUE) // never improves
                    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                    .maxEvaluations(5)
                    .timeStep(DAY)
                    .duration(new Quantity(3, DAY))
                    .build()
                    .execute();

            assertThat(result).isNotNull();
            assertThat(result.getBestRunResult()).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // Fix 2: Objectives.fitToTimeSeries length mismatch (logs warning, doesn't crash)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 2: fitToTimeSeries length mismatch")
    class FitToTimeSeriesFix {

        @Test
        void shouldHandleLongerObservedData() {
            double[] observed = {100, 99, 98, 97, 96, 95, 94, 93, 92, 91};
            var objective = Objectives.fitToTimeSeries("Tank", observed);

            // Run a simulation that produces fewer steps than observed
            Model model = buildSimpleModel(0.1);
            RunResult rr = new RunResult(Map.of("rate", 0.1));
            Simulation sim = new Simulation(model, DAY, DAY, 5);
            sim.addEventHandler(rr);
            sim.execute();

            // Should compute SSE over the min(simulated, observed) length without exception
            double sse = objective.evaluate(rr);
            assertThat(sse).isFinite();
        }

        @Test
        void shouldHandleShorterObservedData() {
            double[] observed = {100, 99};
            var objective = Objectives.fitToTimeSeries("Tank", observed);

            Model model = buildSimpleModel(0.1);
            RunResult rr = new RunResult(Map.of("rate", 0.1));
            Simulation sim = new Simulation(model, DAY, DAY, 10);
            sim.addEventHandler(rr);
            sim.execute();

            double sse = objective.evaluate(rr);
            assertThat(sse).isFinite();
        }
    }

    // -----------------------------------------------------------------------
    // Fix 3: LOOKUP isolated inputHolder (same table, multiple formula references)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 3: LOOKUP isolated input holders")
    class LookupIsolationFix {

        @Test
        void shouldIsolateLookupInputsAcrossFormulas() {
            // Two auxiliaries both reference the same lookup table with different inputs.
            // Without isolation, they would share the same inputHolder and interfere.
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Lookup Isolation")
                    .stock("A", 10, "Thing")
                    .stock("B", 90, "Thing")
                    .lookupTable("Effect", new double[]{0, 50, 100}, new double[]{0.0, 0.5, 1.0}, "LINEAR")
                    .aux("Effect of A", "LOOKUP(Effect, A)", "Thing")
                    .aux("Effect of B", "LOOKUP(Effect, B)", "Thing")
                    .build();

            ModelCompiler compiler = new ModelCompiler();
            CompiledModel compiled = compiler.compile(def);
            Model model = compiled.getModel();

            assertThat(model.getVariable("Effect of A")).isPresent();
            assertThat(model.getVariable("Effect of B")).isPresent();

            Variable effectA = model.getVariable("Effect of A").orElseThrow();
            Variable effectB = model.getVariable("Effect of B").orElseThrow();

            // A=10 → LOOKUP(Effect, 10) ≈ 0.1, B=90 → LOOKUP(Effect, 90) ≈ 0.9
            assertThat(effectA.getValue()).isCloseTo(0.1, within(0.01));
            assertThat(effectB.getValue()).isCloseTo(0.9, within(0.01));
        }
    }

    // -----------------------------------------------------------------------
    // Fix 4: DT configurable via CompiledModel.setDt()
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 4: Configurable DT via dtHolder")
    class ConfigurableDtFix {

        @Test
        void shouldUseDtFromCompiledModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("DT Test")
                    .stock("S", 100, "Thing")
                    .aux("Step Size", "DT", "Thing")
                    .build();

            ModelCompiler compiler = new ModelCompiler();
            CompiledModel compiled = compiler.compile(def);

            // Default DT is 1.0
            assertThat(compiled.getDt()).isEqualTo(1.0);
            Variable stepSize = compiled.getModel().getVariable("Step Size").orElseThrow();
            assertThat(stepSize.getValue()).isEqualTo(1.0);

            // Change DT
            compiled.setDt(0.25);
            assertThat(compiled.getDt()).isEqualTo(0.25);
            assertThat(stepSize.getValue()).isEqualTo(0.25);
        }

        @Test
        void shouldShareDtBetweenParentAndChildContexts() {
            // Module sub-models should see the same DT as the parent
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .moduleInterface(new ModuleInterface(
                            List.of(new PortDef("x", "Thing")),
                            List.of(new PortDef("out", "Thing"))))
                    .aux("out", "x * DT", "Thing")
                    .build();

            ModelDefinition outerDef = new ModelDefinitionBuilder()
                    .name("Outer")
                    .constant("Input", 10, "Thing")
                    .module("m1", innerDef,
                            Map.of("x", "Input"),
                            Map.of("out", "Result"))
                    .build();

            ModelCompiler compiler = new ModelCompiler();
            CompiledModel compiled = compiler.compile(outerDef);

            // Default DT=1.0: Result = 10 * 1.0 = 10
            assertThat(compiled.getModel().getVariable("Result")).isPresent();
            Variable result = compiled.getModel().getVariable("Result").orElseThrow();
            assertThat(result.getValue()).isCloseTo(10.0, within(0.01));

            // Change DT=0.5: Result = 10 * 0.5 = 5
            compiled.setDt(0.5);
            assertThat(result.getValue()).isCloseTo(5.0, within(0.01));
        }
    }

    // -----------------------------------------------------------------------
    // Fix 5: Model.addModule propagates flows
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 5: Model.addModule propagates flows")
    class ModelFlowPropagationFix {

        @Test
        void shouldIncludeModuleFlowsInParentModel() {
            Module module = new Module("SIR");
            Stock s = new Stock("Susceptible", 1000, ItemUnits.PEOPLE);
            Stock i = new Stock("Infectious", 10, ItemUnits.PEOPLE);
            Flow infection = Flow.create("Infection", TimeUnits.DAY,
                    () -> new Quantity(5.0, THING));
            s.addOutflow(infection);
            i.addInflow(infection);

            module.addStock(s);
            module.addStock(i);
            module.addFlow(infection);

            Model model = new Model("Parent");
            model.addModule(module);

            assertThat(model.getFlows()).contains(infection);
            assertThat(model.getFlows()).hasSize(1);
        }

        @Test
        void shouldNotDuplicateFlowsOnRepeatedAdd() {
            Module module = new Module("M");
            Flow flow = Flow.create("F1", TimeUnits.DAY, () -> new Quantity(1.0, THING));
            module.addFlow(flow);

            Model model = new Model("Parent");
            model.addFlow(flow); // add directly first
            model.addModule(module); // module also has the same flow

            assertThat(model.getFlows()).hasSize(1);
        }
    }

    // -----------------------------------------------------------------------
    // Fix 6: DefinitionValidator catches unknown formula references
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 6: Validator catches unknown element references")
    class ValidatorReferenceCheckFix {

        @Test
        void shouldDetectUnknownReferenceInFlowEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad Ref")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * NonExistent", "Day", "S", null)
                    .build();

            List<String> errors = DefinitionValidator.validate(def);
            assertThat(errors).anyMatch(e -> e.contains("unknown element") && e.contains("NonExistent"));
        }

        @Test
        void shouldDetectUnknownReferenceInAuxEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad Ref")
                    .stock("S", 100, "Thing")
                    .aux("Var", "S + Missing", "Thing")
                    .build();

            List<String> errors = DefinitionValidator.validate(def);
            assertThat(errors).anyMatch(e -> e.contains("unknown element") && e.contains("Missing"));
        }

        @Test
        void shouldAllowBuiltinNames() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Builtins OK")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * DT", "Day", "S", null)
                    .aux("T", "TIME", "Thing")
                    .build();

            List<String> errors = DefinitionValidator.validate(def);
            assertThat(errors).as("TIME and DT should be allowed").isEmpty();
        }

        @Test
        void shouldResolveUnderscoreToSpaceInReferences() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Underscore")
                    .stock("S", 100, "Thing")
                    .constant("Birth Rate", 0.03, "Thing")
                    .flow("F", "S * Birth_Rate", "Day", "S", null)
                    .build();

            List<String> errors = DefinitionValidator.validate(def);
            assertThat(errors).as("Birth_Rate should resolve to Birth Rate").isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Fix 7: Module port units resolved from PortDef
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 7: Module port units from PortDef")
    class ModulePortUnitsFix {

        @Test
        void shouldResolvePortUnitFromModuleInterface() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .moduleInterface(new ModuleInterface(
                            List.of(new PortDef("rate", "Dimensionless")),
                            List.of(new PortDef("output", "Person"))))
                    .stock("Pop", 100, "Person")
                    .aux("output", "Pop * rate", "Person")
                    .build();

            ModelDefinition outerDef = new ModelDefinitionBuilder()
                    .name("Outer")
                    .constant("MyRate", 0.5, "Dimensionless")
                    .module("m", innerDef,
                            Map.of("rate", "MyRate"),
                            Map.of("output", "Scaled"))
                    .build();

            ModelCompiler compiler = new ModelCompiler();
            CompiledModel compiled = compiler.compile(outerDef);
            assertThat(compiled.getModel().getVariable("Scaled")).isPresent();
            Variable scaled = compiled.getModel().getVariable("Scaled").orElseThrow();
            assertThat(scaled.getValue()).isCloseTo(50.0, within(0.01));
        }
    }

    // -----------------------------------------------------------------------
    // Fix 8: RunResult unified constructors
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Fix 8: RunResult Map constructor derives parameterValue")
    class RunResultConstructorFix {

        @Test
        void shouldDeriveParameterValueFromFirstMapEntry() {
            RunResult rr = new RunResult(Map.of("rate", 3.14));
            assertThat(rr.getParameterValue()).isEqualTo(3.14);
        }

        @Test
        void shouldReturnZeroForEmptyMap() {
            RunResult rr = new RunResult(Map.of());
            assertThat(rr.getParameterValue()).isEqualTo(0.0);
        }

        @Test
        void shouldExposeFullParameterMap() {
            Map<String, Double> params = Map.of("a", 1.0, "b", 2.0);
            RunResult rr = new RunResult(params);
            assertThat(rr.getParameterMap()).containsAllEntriesOf(params);
        }

        @Test
        void shouldPreserveBackwardCompatWithSingleArgConstructor() {
            RunResult rr = new RunResult(42.0);
            assertThat(rr.getParameterValue()).isEqualTo(42.0);
            assertThat(rr.getParameterMap()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Helper: builds a simple draining-tank model
    // -----------------------------------------------------------------------
    private static Model buildSimpleModel(double drainRate) {
        Model model = new Model("Simple");
        Stock tank = new Stock("Tank", 100, THING);
        model.addStock(tank);
        Flow drain = Flow.create("Drain", TimeUnits.DAY,
                () -> new Quantity(drainRate * tank.getValue(), THING));
        tank.addOutflow(drain);
        model.addFlow(drain);
        return model;
    }
}
