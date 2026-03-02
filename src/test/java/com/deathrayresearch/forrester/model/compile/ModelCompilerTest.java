package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelCompiler")
class ModelCompilerTest {

    private final ModelCompiler compiler = new ModelCompiler();

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

            Model model = compiled.getModel();
            Stock tank = model.getStocks().get(0);
            // After 10 days of 10% drain: 100 * 0.9^10 ≈ 34.87
            assertTrue(tank.getValue() < 40);
            assertTrue(tank.getValue() > 30);
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

            Stock pop = compiled.getModel().getStocks().get(0);
            // After 100 days of 1% growth: 100 * 1.01^100 ≈ 270.48
            assertTrue(pop.getValue() > 260);
            assertTrue(pop.getValue() < 280);
        }
    }

    @Nested
    @DisplayName("SIR model")
    class SIRModel {

        @Test
        void shouldMatchHandBuiltSIR() {
            // Build SIR model as a definition
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
            com.deathrayresearch.forrester.model.Model handModel =
                    new com.deathrayresearch.forrester.model.Model("SIR Hand-built");

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

            // Compare final stock values
            Model compiledModel = compiled.getModel();
            assertEquals(handS.getValue(),
                    compiledModel.getStocks().get(0).getValue(), 0.1,
                    "Susceptible should match");
            assertEquals(handI.getValue(),
                    compiledModel.getStocks().get(1).getValue(), 0.1,
                    "Infectious should match");
            assertEquals(handR.getValue(),
                    compiledModel.getStocks().get(2).getValue(), 0.1,
                    "Recovered should match");
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
            // Should not throw
            sim.execute();
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
            Stock s = compiled.getModel().getStocks().get(0);
            assertTrue(s.getValue() > 50); // 6 steps * 10 = 60
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

            Stock s = compiled.getModel().getStocks().get(0);
            assertTrue(s.getValue() > 0);
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
            // Should not throw
            sim.execute();

            Stock s = compiled.getModel().getStocks().get(0);
            assertTrue(s.getValue() < 50, "Stock should have drained");
        }
    }
}
