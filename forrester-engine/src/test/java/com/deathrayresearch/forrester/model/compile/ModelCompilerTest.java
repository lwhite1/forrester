package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
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

            com.deathrayresearch.forrester.model.Flow infectionRate =
                    com.deathrayresearch.forrester.model.Flow.create("Infection", DAY, () -> {
                        double total = handS.getValue() + handI.getValue() + handR.getValue();
                        double rate = 8.0 * handI.getValue() / total * 0.10 * handS.getValue();
                        return new Quantity(rate, PEOPLE);
                    });
            com.deathrayresearch.forrester.model.Flow recoveryRate =
                    com.deathrayresearch.forrester.model.Flow.create("Recovery", DAY, () ->
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
                    .aux("A", "S * 2", "Thing")
                    .aux("B", "A + 10", "Thing")
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
                    .hasMessageContaining("unknown source");
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
                    .hasMessageContaining("unknown sink");
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
                            com.deathrayresearch.forrester.model.expr.ParseException.class);
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
                    .aux("output", "Value * 2", "Thing")
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
}
