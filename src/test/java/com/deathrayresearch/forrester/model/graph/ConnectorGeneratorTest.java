package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConnectorGenerator")
class ConnectorGeneratorTest {

    @Test
    void shouldGenerateConnectorsFromFormulaDependencies() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Simple")
                .stock("Tank", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        assertFalse(connectors.isEmpty());
        // Should have connectors for: Tank → Drain, Rate → Drain, Drain → Tank
        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());
        assertTrue(keys.contains("Tank -> Drain"), "Tank influences Drain");
        assertTrue(keys.contains("Rate -> Drain"), "Rate influences Drain");
        assertTrue(keys.contains("Drain -> Tank"), "Drain connects to Tank (source)");
    }

    @Test
    void shouldEliminateDuplicateConnectors() {
        // A flow that references the same stock in its formula AND as source/sink
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("DupTest")
                .stock("S", 100, "Thing")
                .flow("F", "S * 0.1", "Day", "S", null)
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        // Count connectors from S → F (formula dep) — should appear only once
        long sToFCount = connectors.stream()
                .filter(c -> c.from().equals("S") && c.to().equals("F"))
                .count();
        assertEquals(1, sToFCount, "S → F should appear only once");
    }

    @Test
    void shouldGenerateConnectorsForSIRModel() {
        ModelDefinition sir = new ModelDefinitionBuilder()
                .name("SIR")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .constant("Contact Rate", 8, "Dimensionless unit")
                .constant("Infectivity", 0.5, "Dimensionless unit")
                .constant("Duration", 5, "Day")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious / Duration", "Day", "Infectious", "Recovered")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(sir);

        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());

        // Infection formula refs
        assertTrue(keys.contains("Contact Rate -> Infection"));
        assertTrue(keys.contains("Infectivity -> Infection"));
        assertTrue(keys.contains("Susceptible -> Infection"));
        assertTrue(keys.contains("Infectious -> Infection"));
        assertTrue(keys.contains("Recovered -> Infection"));

        // Infection flow connections
        assertTrue(keys.contains("Infection -> Susceptible")); // source
        assertTrue(keys.contains("Infection -> Infectious"));  // sink

        // Recovery formula refs
        assertTrue(keys.contains("Infectious -> Recovery"));
        assertTrue(keys.contains("Duration -> Recovery"));

        // Recovery flow connections
        assertTrue(keys.contains("Recovery -> Infectious")); // source
        assertTrue(keys.contains("Recovery -> Recovered"));  // sink
    }

    @Test
    void shouldReturnEmptyForModelWithNoEquations() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("ConstantsOnly")
                .constant("A", 1, "Thing")
                .constant("B", 2, "Thing")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
        assertTrue(connectors.isEmpty());
    }

    @Test
    void shouldHandleAuxiliaryDependencies() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("WithAux")
                .stock("S", 100, "Thing")
                .constant("C", 2, "Thing")
                .aux("Total", "S + C", "Thing")
                .build();

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        Set<String> keys = connectors.stream()
                .map(c -> c.from() + " -> " + c.to())
                .collect(Collectors.toSet());

        assertTrue(keys.contains("S -> Total"), "S influences Total");
        assertTrue(keys.contains("C -> Total"), "C influences Total");
    }
}
