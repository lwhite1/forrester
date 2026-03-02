package com.deathrayresearch.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelDefinitionBuilder")
class ModelDefinitionBuilderTest {

    @Test
    void shouldBuildSIRModel() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR Model")
                .comment("Classic SIR epidemiological model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless")
                .constant("Infectivity", 0.10, "Dimensionless")
                .constant("Recovery_Rate", 0.20, "Dimensionless")
                .defaultSimulation("Day", 56, "Day")
                .build();

        assertEquals("SIR Model", sir.name());
        assertEquals(3, sir.stocks().size());
        assertEquals(2, sir.flows().size());
        assertEquals(3, sir.constants().size());
        assertNotNull(sir.defaultSimulation());
        assertEquals(56, sir.defaultSimulation().duration());
    }

    @Test
    void shouldBuildEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();
        assertEquals("Empty", def.name());
        assertTrue(def.stocks().isEmpty());
        assertTrue(def.flows().isEmpty());
        assertTrue(def.auxiliaries().isEmpty());
        assertTrue(def.constants().isEmpty());
    }

    @Test
    void shouldBuildModelWithAuxiliaries() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Aux Test")
                .stock("Population", 1000, "Person")
                .aux("Birth Rate", "Population * Fertility", "Person")
                .constant("Fertility", 0.03, "Dimensionless")
                .build();
        assertEquals(1, def.auxiliaries().size());
        assertEquals("Birth Rate", def.auxiliaries().get(0).name());
    }

    @Test
    void shouldBuildModelWithLookupTable() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Lookup Test")
                .lookupTable("Effect of Crowding",
                        new double[]{0, 0.5, 1.0, 1.5, 2.0},
                        new double[]{1.0, 0.9, 0.7, 0.4, 0.1},
                        "LINEAR")
                .build();
        assertEquals(1, def.lookupTables().size());
        assertEquals(5, def.lookupTables().get(0).xValues().length);
    }

    @Test
    void shouldBuildModelWithSimulationSettings() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Sim Test")
                .defaultSimulation("Week", 52, "Week")
                .build();
        assertEquals("Week", def.defaultSimulation().timeStep());
        assertEquals(52, def.defaultSimulation().duration());
    }

    @Test
    void shouldValidateSuccessfully() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR Model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless")
                .constant("Infectivity", 0.10, "Dimensionless")
                .constant("Recovery_Rate", 0.20, "Dimensionless")
                .build();

        List<String> errors = DefinitionValidator.validate(sir);
        assertTrue(errors.isEmpty(), "SIR model should validate: " + errors);
    }
}
