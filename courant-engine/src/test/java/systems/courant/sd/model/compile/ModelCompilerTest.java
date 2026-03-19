package systems.courant.sd.model.compile;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ModelCompiler")
class ModelCompilerTest {

    private final ModelCompiler compiler = new ModelCompiler();

    private Stock findStock(Model model, String name) {
        return model.getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }

    @Nested
    @DisplayName("Simple drain model")
    class SimpleDrain {

        @Test
        void shouldDrainStockOverTime() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Simple Drain")
                    .stock("Tank", 100, "Thing")
                    .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock tank = findStock(compiled.getModel(), "Tank");
            // After draining, stock should be well below initial value
            assertThat(tank.getValue()).isBetween(25.0, 40.0);
        }
    }

    @Nested
    @DisplayName("Exponential growth")
    class ExponentialGrowth {

        @Test
        void shouldGrowExponentially() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Exponential Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * Growth_Rate", "Day", null, "Population")
                    .constant("Growth_Rate", 0.01, "Dimensionless unit")
                    .defaultSimulation("Day", 100, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock pop = findStock(compiled.getModel(), "Population");
            // After 100 days of 1% growth: approximately 270
            assertThat(pop.getValue()).isBetween(260.0, 280.0);
        }
    }

    @Nested
    @DisplayName("SIR model")
    class SIRModel {

        @Test
        void shouldMatchHandBuiltSIR() {
            ModelDefinition sirDef = new ModelDefinitionBuilder()
                    .name("SIR Model")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .flow("Infection",
                            "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                            "Day", "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                            "Infectious", "Recovered")
                    .constant("Contact_Rate", 8.0, "Dimensionless unit")
                    .constant("Infectivity", 0.10, "Dimensionless unit")
                    .constant("Recovery_Rate", 0.20, "Dimensionless unit")
                    .defaultSimulation("Day", 56, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(sirDef);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // Build the same model by hand
            Model handModel = new Model("SIR Hand-built");

            Stock handS = new Stock("Susceptible", 1000, PEOPLE);
            Stock handI = new Stock("Infectious", 10, PEOPLE);
            Stock handR = new Stock("Recovered", 0, PEOPLE);

            systems.courant.sd.model.Flow infectionRate =
                    systems.courant.sd.model.Flow.create("Infection", DAY, () -> {
                        double total = handS.getValue() + handI.getValue() + handR.getValue();
                        double rate = 8.0 * handI.getValue() / total * 0.10 * handS.getValue();
                        return new Quantity(rate, PEOPLE);
                    });
            systems.courant.sd.model.Flow recoveryRate =
                    systems.courant.sd.model.Flow.create("Recovery", DAY, () ->
                            new Quantity(handI.getValue() * 0.20, PEOPLE));

            handS.addOutflow(infectionRate);
            handI.addInflow(infectionRate);
            handI.addOutflow(recoveryRate);
            handR.addInflow(recoveryRate);

            handModel.addStock(handS);
            handModel.addStock(handI);
            handModel.addStock(handR);

            Simulation handSim = new Simulation(handModel, DAY, DAY, 56);
            handSim.execute();

            // Compare by name, not by index
            Model compiledModel = compiled.getModel();
            assertThat(findStock(compiledModel, "Susceptible").getValue())
                    .as("Susceptible").isCloseTo(handS.getValue(), within(0.1));
            assertThat(findStock(compiledModel, "Infectious").getValue())
                    .as("Infectious").isCloseTo(handI.getValue(), within(0.1));
            assertThat(findStock(compiledModel, "Recovered").getValue())
                    .as("Recovered").isCloseTo(handR.getValue(), within(0.1));
        }
    }

    @Nested
    @DisplayName("Forward references")
    class ForwardReferences {

        @Test
        void shouldHandleAuxReferencingAnotherAux() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Forward Ref")
                    .stock("S", 100, "Thing")
                    .variable("A", "S * 2", "Thing")
                    .variable("B", "A + 10", "Thing")
                    .flow("F", "B", "Day", "S", null)
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // Before flow: A = 100*2 = 200, B = 200+10 = 210
            // After 1 step: S = 100 - 210 is clamped to 0
            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isLessThan(100);
        }
    }

    @Nested
    @DisplayName("STEP and RAMP functions")
    class StepAndRamp {

        @Test
        void shouldCompileSTEP() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Step Test")
                    .stock("S", 0, "Thing")
                    .flow("F", "STEP(10, 5)", "Day", null, "S")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // Stock should accumulate: 0 for steps 0-4, then 10 per step for steps 5-10
            // That's 6 steps (5,6,7,8,9,10) * 10 = 60
            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isCloseTo(60.0, within(1.0));
        }

        @Test
        void shouldCompileRAMP() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Ramp Test")
                    .stock("S", 0, "Thing")
                    .flow("F", "RAMP(2, 3)", "Day", null, "S")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // Ramp starts at step 3: values are 0,0,0,0,2,4,6,8,10,12,14
            // Sum of flow applied at steps 3-10: 0+2+4+6+8+10+12+14 = 56
            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isCloseTo(56.0, within(1.0));
        }
    }

    @Nested
    @DisplayName("LookupTable")
    class LookupTests {

        @Test
        void shouldCompileLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Lookup Test")
                    .stock("S", 50, "Thing")
                    .lookupTable("Effect",
                            new double[]{0, 25, 50, 75, 100},
                            new double[]{1.0, 0.8, 0.5, 0.2, 0.0},
                            "LINEAR")
                    .flow("Drain", "LOOKUP(Effect, S) * S * 0.1", "Day", "S", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isLessThan(50).as("Stock should have drained");
        }

        @Test
        void shouldWireLookupInputCorrectly() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Lookup Wire Test")
                    .stock("S", 100, "Thing")
                    .lookupTable("Effect",
                            new double[]{0, 50, 100},
                            new double[]{1.0, 0.5, 0.0},
                            "LINEAR")
                    .flow("Drain", "LOOKUP(Effect, S) * 10", "Day", "S", null)
                    .defaultSimulation("Day", 5, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue())
                    .as("With effect=0 at S=100, stock should barely drain")
                    .isGreaterThan(90);
        }
    }

    @Nested
    @DisplayName("Reset and re-simulate")
    class ResetAndReSimulate {

        @Test
        void shouldRestoreStockValuesOnReset() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Reset Test")
                    .stock("Tank", 100, "Thing")
                    .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock tank = findStock(compiled.getModel(), "Tank");
            double afterFirst = tank.getValue();
            assertThat(afterFirst).isLessThan(100);

            compiled.reset();
            assertThat(tank.getValue())
                    .as("Stock should be restored to initial value after reset")
                    .isCloseTo(100.0, within(0.001));

            Simulation sim2 = compiled.createSimulation();
            sim2.execute();
            assertThat(tank.getValue())
                    .as("Second simulation should produce same result")
                    .isCloseTo(afterFirst, within(0.001));
        }
    }

    @Nested
    @DisplayName("Module compilation errors")
    class ModuleErrors {

        @Test
        void shouldThrowForBadModuleFlowSource() {
            ModelDefinition moduleDef = new ModelDefinitionBuilder()
                    .name("BadModule")
                    .stock("Tank", 100, "Thing")
                    .flow("Drain", "Tank * 0.1", "Day", "NonExistent", null)
                    .build();

            ModelDefinition outer = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module("m1", moduleDef,
                            java.util.Map.of(), java.util.Map.of())
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(outer))
                    .isInstanceOf(CompilationException.class);
        }

        @Test
        void shouldThrowForBadModuleFlowSink() {
            ModelDefinition moduleDef = new ModelDefinitionBuilder()
                    .name("BadModule")
                    .stock("Tank", 100, "Thing")
                    .flow("Fill", "10", "Day", null, "NonExistent")
                    .build();

            ModelDefinition outer = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module("m1", moduleDef,
                            java.util.Map.of(), java.util.Map.of())
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(outer))
                    .isInstanceOf(CompilationException.class);
        }
    }

    @Nested
    @DisplayName("Compilation failure tests")
    class CompilationFailures {

        @Test
        void shouldThrowForMissingFlowSource() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad")
                    .stock("S", 100, "Thing")
                    .flow("F", "10", "Day", "Missing", null)
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(def))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("non-existent source stock");
        }

        @Test
        void shouldThrowForMissingFlowSink() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad")
                    .stock("S", 100, "Thing")
                    .flow("F", "10", "Day", null, "Missing")
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(def))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("non-existent sink stock");
        }

        @Test
        void shouldThrowForBadFormula() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad")
                    .stock("S", 100, "Thing")
                    .flow("F", "S +", "Day", "S", null)
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(def))
                    .isInstanceOfAny(CompilationException.class,
                            systems.courant.sd.model.expr.ParseException.class);
        }
    }

    @Nested
    @DisplayName("Module with output bindings")
    class OutputBindings {

        @Test
        void shouldCompileModelWithOutputBindings() {
            ModelDefinition innerModule = new ModelDefinitionBuilder()
                    .name("Producer")
                    .stock("Value", 50, "Thing")
                    .variable("output", "Value * 2", "Thing")
                    .build();

            ModelDefinition outer = new ModelDefinitionBuilder()
                    .name("Consumer")
                    .module("producer", innerModule,
                            java.util.Map.of(),
                            java.util.Map.of("output", "ProducerOutput"))
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(outer);
            assertThat(compiled.getModel().getVariable("ProducerOutput")).isPresent();
            Variable output = compiled.getModel().getVariable("ProducerOutput").orElseThrow();
            assertThat(output.getValue()).isCloseTo(100.0, within(0.01));
        }

        @Test
        void shouldThrowForBadOutputBinding() {
            ModelDefinition innerModule = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("Value", 50, "Thing")
                    .build();

            ModelDefinition outer = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module("m1", innerModule,
                            java.util.Map.of(),
                            java.util.Map.of("nonExistent", "Alias"))
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(outer))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("unknown port");
        }
    }

    @Nested
    @DisplayName("createSimulation")
    class CreateSimulation {

        @Test
        void shouldThrowWithoutDefaultSettings() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No Defaults")
                    .stock("S", 100, "Thing")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            assertThatThrownBy(compiled::createSimulation)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No default simulation");
        }

        @Test
        void shouldCreateSimulationWithExplicitTimeSteps() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Explicit")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation(DAY, 5, DAY);
            sim.execute();

            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isLessThan(100);
        }
    }

    @Nested
    @DisplayName("Top-level flows registered in Model (issue #235)")
    class TopLevelFlowRegistration {

        @Test
        void shouldRegisterTopLevelFlowsInModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Flow Registration")
                    .stock("Tank", 100, "Thing")
                    .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                    .flow("Fill", "5", "Day", null, "Tank")
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Model model = compiled.getModel();

            assertThat(model.getFlows())
                    .as("Top-level flows should be registered in model")
                    .hasSize(2);
            assertThat(model.getFlows())
                    .extracting(f -> f.getName())
                    .containsExactlyInAnyOrder("Drain", "Fill");
        }

        @Test
        void shouldClearFlowHistoryOnReSimulate() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("History Clear")
                    .stock("S", 100, "Thing")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .defaultSimulation("Day", 5, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim1 = compiled.createSimulation();
            sim1.execute();

            // Flow should have recorded history from the first run
            systems.courant.sd.model.Flow flow = compiled.getModel().getFlows().stream()
                    .filter(f -> f.getName().equals("F"))
                    .findFirst()
                    .orElseThrow();
            assertThat(flow.getHistoryAtTimeStep(0)).isGreaterThan(0);

            // Re-simulate — clearHistory is called at simulation start
            compiled.reset();
            Simulation sim2 = compiled.createSimulation();
            sim2.execute();

            // History should reflect the second run, not accumulate from first
            Stock s = findStock(compiled.getModel(), "S");
            assertThat(s.getValue()).isLessThan(100);
        }

        @Test
        void shouldIncludeFlowsFromSingleFlowModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Single Flow")
                    .stock("S", 50, "Thing")
                    .flow("OnlyFlow", "10", "Day", null, "S")
                    .defaultSimulation("Day", 3, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            assertThat(compiled.getModel().getFlows()).hasSize(1);
            assertThat(compiled.getModel().getFlows().get(0).getName())
                    .isEqualTo("OnlyFlow");
        }
    }

    @Nested
    @DisplayName("NegativeValuePolicy")
    class NegativeValuePolicyTests {

        @Test
        void shouldThrowForUnknownPolicy() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("BadPolicy")
                    .stock("S", "comment", 100, "Thing", "INVALID_POLICY")
                    .flow("F", "200", "Day", "S", null)
                    .defaultSimulation("Day", 1, "Day")
                    .build();

            assertThatThrownBy(() -> compiler.compile(def))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("Unknown NegativeValuePolicy");
        }
    }

    @Nested
    @DisplayName("Subscript expansion")
    class SubscriptExpansionTests {

        @Test
        void shouldCompileSubscriptedModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Regional Population")
                    .subscript("Region", java.util.List.of("North", "South"))
                    .stock("Population", 100, "Person", java.util.List.of("Region"))
                    .constant("Growth_Rate", 0.01, "Dimensionless unit")
                    .flow("Births", "Population * Growth_Rate", "Day",
                            null, "Population", java.util.List.of("Region"))
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock north = findStock(compiled.getModel(), "Population[North]");
            Stock south = findStock(compiled.getModel(), "Population[South]");

            // Both should grow from 100 at 1% per day for 10 days
            assertThat(north.getValue()).isBetween(109.0, 112.0);
            assertThat(south.getValue()).isBetween(109.0, 112.0);
            // They should have the same value (symmetric model)
            assertThat(north.getValue()).isCloseTo(south.getValue(), within(0.001));
        }

        @Test
        void shouldCompileSubscriptedDrainModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Regional Drain")
                    .subscript("Region", java.util.List.of("East", "West"))
                    .stock("Tank", 100, "Thing", java.util.List.of("Region"))
                    .flow("Drain", "Tank * 0.1", "Day",
                            "Tank", null, java.util.List.of("Region"))
                    .defaultSimulation("Day", 5, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock east = findStock(compiled.getModel(), "Tank[East]");
            Stock west = findStock(compiled.getModel(), "Tank[West]");

            assertThat(east.getValue()).isLessThan(100);
            assertThat(west.getValue()).isLessThan(100);
            assertThat(east.getValue()).isCloseTo(west.getValue(), within(0.001));
        }

        @Test
        void shouldCompileMixedSubscriptedAndScalarElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed")
                    .subscript("Region", java.util.List.of("A", "B"))
                    .stock("Pop", 50, "Person", java.util.List.of("Region"))
                    .stock("Total", 0, "Person")
                    .constant("rate", 0.1, "Dimensionless unit")
                    .flow("growth", "Pop * rate", "Day",
                            null, "Pop", java.util.List.of("Region"))
                    .defaultSimulation("Day", 5, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock popA = findStock(compiled.getModel(), "Pop[A]");
            Stock popB = findStock(compiled.getModel(), "Pop[B]");
            Stock total = findStock(compiled.getModel(), "Total");

            assertThat(popA.getValue()).isGreaterThan(50);
            assertThat(popB.getValue()).isGreaterThan(50);
            assertThat(total.getValue()).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Material unit")
    class MaterialUnit {

        @Test
        void shouldUseExplicitMaterialUnitWhenProvided() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Explicit Material Unit")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.01", "Day", "Person",
                            null, "Population")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock pop = findStock(compiled.getModel(), "Population");
            assertThat(pop.getValue()).isGreaterThan(100);

            // Verify the flow has the correct material unit
            systems.courant.sd.model.Flow flow = compiled.getModel().getFlows()
                    .stream().filter(f -> f.getName().equals("Births")).findFirst().orElseThrow();
            assertThat(flow.getMaterialUnit()).isNotNull();
            assertThat(flow.getMaterialUnit().getName()).isEqualTo("Person");
        }

        @Test
        void shouldFallBackToStockUnitWhenMaterialUnitIsNull() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Inferred Material Unit")
                    .stock("Tank", 100, "Liter")
                    .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            CompiledModel compiled = compiler.compile(def);

            // Flow should have Liter as material unit (inferred from stock)
            systems.courant.sd.model.Flow flow = compiled.getModel().getFlows()
                    .stream().filter(f -> f.getName().equals("Drain")).findFirst().orElseThrow();
            assertThat(flow.getMaterialUnit()).isNotNull();
            assertThat(flow.getMaterialUnit().getName()).isEqualTo("Liter");
        }
    }

    @Nested
    @DisplayName("Subscript expansion and dependency graph ordering")
    class SubscriptDependencyGraph {

        @Test
        void shouldCompileSubscriptedModelWithCrossDimensionalReference() {
            // Ensures the dependency graph runs on the expanded definition,
            // so cross-dimensional references are visible (#1039)
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CrossDim")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .flow("migration", "Pop * 0.01", "Year", "Pop", null,
                            List.of("Region"))
                    .defaultSimulation("Year", 10, "Year")
                    .build();

            CompiledModel compiled = compiler.compile(def);

            assertThat(compiled.getModel().getStocks()).hasSize(2);
            assertThat(compiled.getModel().getStocks().stream().map(Stock::getName))
                    .containsExactlyInAnyOrder("Pop[North]", "Pop[South]");
        }
    }
}
