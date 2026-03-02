package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DependencyGraph")
class DependencyGraphTest {

    @Test
    void shouldExtractDependenciesFromSimpleModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Simple Drain")
                .stock("Tank", 100, "Thing")
                .constant("Rate", 0.1, "Dimensionless unit")
                .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        // Tank and Rate influence Drain (formula deps)
        Set<String> drainDeps = graph.dependenciesOf("Drain");
        assertTrue(drainDeps.contains("Tank"), "Tank should be a dependency of Drain");
        assertTrue(drainDeps.contains("Rate"), "Rate should be a dependency of Drain");

        // Drain influences Tank (flow→source connection)
        Set<String> drainDependents = graph.dependentsOf("Drain");
        assertTrue(drainDependents.contains("Tank"), "Drain should flow into Tank (source)");
    }

    @Test
    void shouldExtractSIRModelDependencies() {
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

        DependencyGraph graph = DependencyGraph.fromDefinition(sir);

        // Infection should depend on Contact Rate, Infectious, Susceptible, Recovered, Infectivity
        Set<String> infectionDeps = graph.dependenciesOf("Infection");
        assertTrue(infectionDeps.contains("Contact Rate"), "Infection depends on Contact Rate");
        assertTrue(infectionDeps.contains("Infectious"), "Infection depends on Infectious");
        assertTrue(infectionDeps.contains("Susceptible"), "Infection depends on Susceptible");
        assertTrue(infectionDeps.contains("Recovered"), "Infection depends on Recovered");
        assertTrue(infectionDeps.contains("Infectivity"), "Infection depends on Infectivity");

        // Recovery depends on Infectious and Duration
        Set<String> recoveryDeps = graph.dependenciesOf("Recovery");
        assertTrue(recoveryDeps.contains("Infectious"), "Recovery depends on Infectious");
        assertTrue(recoveryDeps.contains("Duration"), "Recovery depends on Duration");
    }

    @Test
    void shouldReturnAllNodes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .constant("C", 5, "Thing")
                .flow("F", "S * C", "Day", "S", null)
                .aux("A", "S + C", "Thing")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> nodes = graph.allNodes();

        assertEquals(4, nodes.size());
        assertTrue(nodes.contains("S"));
        assertTrue(nodes.contains("C"));
        assertTrue(nodes.contains("F"));
        assertTrue(nodes.contains("A"));
    }

    @Test
    void shouldReturnAllEdges() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .flow("F", "S", "Day", "S", null)
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        List<String[]> edges = graph.allEdges();

        assertFalse(edges.isEmpty());
        // S → F (formula dependency) and F → S (flow source connection)
        boolean hasStoF = edges.stream().anyMatch(e -> e[0].equals("S") && e[1].equals("F"));
        boolean hasFtoS = edges.stream().anyMatch(e -> e[0].equals("F") && e[1].equals("S"));
        assertTrue(hasStoF, "Should have edge S → F (formula dep)");
        assertTrue(hasFtoS, "Should have edge F → S (source connection)");
    }

    @Test
    void shouldProduceTopologicalSort() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Chain")
                .constant("C", 10, "Thing")
                .aux("A", "C * 2", "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "A", "Day", null, "S")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        List<String> sorted = graph.topologicalSort();

        // C should come before A (C influences A)
        assertTrue(sorted.indexOf("C") < sorted.indexOf("A"),
                "C should precede A in topological sort");
    }

    @Test
    void shouldDetectCycleInStockFlowLoop() {
        // Stock-flow feedback loops are expected in SD models
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Feedback")
                .stock("Population", 100, "Person")
                .flow("Births", "Population * 0.04", "Day", null, "Population")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        // Population → Births (formula dep) and Births → Population (flow sink)
        // This creates a cycle
        assertTrue(graph.hasCycle(), "Stock-flow feedback loop should be detected as a cycle");
    }

    @Test
    void shouldNotDetectCycleInAcyclicModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Acyclic")
                .constant("Rate", 5, "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "Rate", "Day", null, "S")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        assertFalse(graph.hasCycle(), "Acyclic model should not have cycles");
    }

    @Test
    void shouldHandleAuxiliaryDependencies() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("AuxDeps")
                .stock("S", 100, "Thing")
                .constant("C", 2, "Thing")
                .aux("A1", "S * C", "Thing")
                .aux("A2", "A1 + 10", "Thing")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        // A1 depends on S and C
        Set<String> a1Deps = graph.dependenciesOf("A1");
        assertTrue(a1Deps.contains("S"));
        assertTrue(a1Deps.contains("C"));

        // A2 depends on A1
        Set<String> a2Deps = graph.dependenciesOf("A2");
        assertTrue(a2Deps.contains("A1"));

        // S influences A1
        Set<String> sDependents = graph.dependentsOf("S");
        assertTrue(sDependents.contains("A1"));
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        assertTrue(graph.allNodes().isEmpty());
        assertTrue(graph.allEdges().isEmpty());
        assertFalse(graph.hasCycle());
        assertTrue(graph.topologicalSort().isEmpty());
    }
}
