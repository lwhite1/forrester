package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(drainDeps.contains("Tank")).as("Tank should be a dependency of Drain").isTrue();
        assertThat(drainDeps.contains("Rate")).as("Rate should be a dependency of Drain").isTrue();

        // Drain influences Tank (flow→source connection)
        Set<String> drainDependents = graph.dependentsOf("Drain");
        assertThat(drainDependents.contains("Tank")).as("Drain should flow into Tank (source)").isTrue();
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
        assertThat(infectionDeps.contains("Contact Rate")).as("Infection depends on Contact Rate").isTrue();
        assertThat(infectionDeps.contains("Infectious")).as("Infection depends on Infectious").isTrue();
        assertThat(infectionDeps.contains("Susceptible")).as("Infection depends on Susceptible").isTrue();
        assertThat(infectionDeps.contains("Recovered")).as("Infection depends on Recovered").isTrue();
        assertThat(infectionDeps.contains("Infectivity")).as("Infection depends on Infectivity").isTrue();

        // Recovery depends on Infectious and Duration
        Set<String> recoveryDeps = graph.dependenciesOf("Recovery");
        assertThat(recoveryDeps.contains("Infectious")).as("Recovery depends on Infectious").isTrue();
        assertThat(recoveryDeps.contains("Duration")).as("Recovery depends on Duration").isTrue();
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

        assertThat(nodes.size()).isEqualTo(4);
        assertThat(nodes.contains("S")).isTrue();
        assertThat(nodes.contains("C")).isTrue();
        assertThat(nodes.contains("F")).isTrue();
        assertThat(nodes.contains("A")).isTrue();
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

        assertThat(edges.isEmpty()).isFalse();
        // S → F (formula dependency) and F → S (flow source connection)
        boolean hasStoF = edges.stream().anyMatch(e -> e[0].equals("S") && e[1].equals("F"));
        boolean hasFtoS = edges.stream().anyMatch(e -> e[0].equals("F") && e[1].equals("S"));
        assertThat(hasStoF).as("Should have edge S → F (formula dep)").isTrue();
        assertThat(hasFtoS).as("Should have edge F → S (source connection)").isTrue();
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
        assertThat(sorted.indexOf("C") < sorted.indexOf("A"))
                .as("C should precede A in topological sort").isTrue();
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
        assertThat(graph.hasCycle()).as("Stock-flow feedback loop should be detected as a cycle").isTrue();
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
        assertThat(graph.hasCycle()).as("Acyclic model should not have cycles").isFalse();
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
        assertThat(a1Deps.contains("S")).isTrue();
        assertThat(a1Deps.contains("C")).isTrue();

        // A2 depends on A1
        Set<String> a2Deps = graph.dependenciesOf("A2");
        assertThat(a2Deps.contains("A1")).isTrue();

        // S influences A1
        Set<String> sDependents = graph.dependentsOf("S");
        assertThat(sDependents.contains("A1")).isTrue();
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        assertThat(graph.allNodes().isEmpty()).isTrue();
        assertThat(graph.allEdges().isEmpty()).isTrue();
        assertThat(graph.hasCycle()).isFalse();
        assertThat(graph.topologicalSort().isEmpty()).isTrue();
    }
}
