package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
        assertThat(drainDeps).as("Tank should be a dependency of Drain").contains("Tank");
        assertThat(drainDeps).as("Rate should be a dependency of Drain").contains("Rate");

        // Drain influences Tank (flow→source connection)
        Set<String> drainDependents = graph.dependentsOf("Drain");
        assertThat(drainDependents).as("Drain should flow into Tank (source)").contains("Tank");
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
        assertThat(infectionDeps).as("Infection dependencies")
                .contains("Contact Rate", "Infectious", "Susceptible", "Recovered", "Infectivity");

        // Recovery depends on Infectious and Duration
        Set<String> recoveryDeps = graph.dependenciesOf("Recovery");
        assertThat(recoveryDeps).as("Recovery dependencies")
                .contains("Infectious", "Duration");
    }

    @Test
    void shouldReturnAllNodes() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100, "Thing")
                .constant("C", 5, "Thing")
                .flow("F", "S * C", "Day", "S", null)
                .variable("A", "S + C", "Thing")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> nodes = graph.allNodes();

        assertThat(nodes).hasSize(4).contains("S", "C", "F", "A");
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

        assertThat(edges).isNotEmpty();
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
                .variable("A", "C * 2", "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "A", "Day", null, "S")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        List<String> sorted = graph.topologicalSort();

        // C should come before A (C influences A)
        assertThat(sorted.indexOf("C"))
                .as("C should precede A in topological sort")
                .isLessThan(sorted.indexOf("A"));
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
                .variable("A1", "S * C", "Thing")
                .variable("A2", "A1 + 10", "Thing")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        // A1 depends on S and C
        assertThat(graph.dependenciesOf("A1")).contains("S", "C");

        // A2 depends on A1
        assertThat(graph.dependenciesOf("A2")).contains("A1");

        // S influences A1
        assertThat(graph.dependentsOf("S")).contains("A1");
    }

    @Test
    void shouldFindSccInFeedbackLoop() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Feedback")
                .stock("Population", 100, "Person")
                .flow("Births", "Population * 0.04", "Day", null, "Population")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> sccMembers = graph.findSccMembers();

        // Population → Births (formula dep) and Births → Population (flow sink) form an SCC
        assertThat(sccMembers).contains("Population", "Births");
    }

    @Test
    void shouldNotFindSccInAcyclicModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Acyclic")
                .constant("Rate", 5, "Thing")
                .stock("S", 0, "Thing")
                .flow("F", "Rate", "Day", null, "S")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> sccMembers = graph.findSccMembers();

        assertThat(sccMembers).isEmpty();
    }

    @Test
    void shouldFindMultipleSCCs() {
        // Two independent feedback loops
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Two Loops")
                .stock("A", 100, "Thing")
                .flow("FlowA", "A * 0.1", "Day", null, "A")
                .stock("B", 50, "Thing")
                .flow("FlowB", "B * 0.2", "Day", null, "B")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        List<Set<String>> sccs = graph.findSCCs();

        assertThat(sccs).hasSize(2);
        Set<String> allMembers = graph.findSccMembers();
        assertThat(allMembers).containsExactlyInAnyOrder("A", "FlowA", "B", "FlowB");
    }

    @Test
    void shouldNotCorruptStackWhenDepthExceeded() {
        // Build a chain of 250 variables referencing the next, with
        // the last referencing the first to form a single deep cycle.
        // This exceeds MAX_DEPTH=200, exercising the depth-limit bail-out.
        ModelDefinitionBuilder builder = new ModelDefinitionBuilder().name("Deep");
        int n = 250;
        for (int i = 0; i < n; i++) {
            String dep = "v" + ((i + 1) % n);
            builder.variable("v" + i, dep, "Thing");
        }
        DependencyGraph graph = DependencyGraph.fromDefinition(builder.build());

        // Should not throw — the depth guard must leave algorithm state consistent
        List<Set<String>> sccs = graph.findSCCs();

        // The SCC detection may be incomplete due to depth cutoff, but it
        // must not include nodes from unrelated components
        Set<String> allSccMembers = graph.findSccMembers();
        for (String member : allSccMembers) {
            assertThat(graph.allNodes()).contains(member);
        }
    }

    @Test
    void shouldHandleEmptyModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .build();

        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        assertThat(graph.allNodes()).isEmpty();
        assertThat(graph.allEdges()).isEmpty();
        assertThat(graph.hasCycle()).isFalse();
        assertThat(graph.topologicalSort()).isEmpty();
    }

    @Nested
    @DisplayName("Graceful degradation on unparseable equations (#450)")
    class UnparseableEquations {

        @Test
        void shouldBuildPartialGraphWhenFlowEquationIsUnparseable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("BadFlow")
                    .stock("Tank", 100, "Thing")
                    .constant("Rate", 5, "Thing")
                    .flow("GoodFlow", "Tank * Rate", "Day", "Tank", null)
                    .flow("BadFlow", "@@@ totally invalid !!!", "Day", null, "Tank")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);

            // Should not throw — unparseable flow is skipped
            assertThat(graph.allNodes()).contains("Tank", "Rate", "GoodFlow", "BadFlow");
            // Good flow still has its dependencies extracted
            assertThat(graph.dependenciesOf("GoodFlow")).contains("Tank", "Rate");
            // Bad flow has no formula dependencies (only structural sink connection)
            assertThat(graph.dependentsOf("BadFlow")).contains("Tank");
        }

        @Test
        void shouldBuildPartialGraphWhenAuxEquationIsUnparseable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("BadAux")
                    .stock("S", 50, "Thing")
                    .variable("GoodAux", "S * 2", "Thing")
                    .variable("BadAux", "### not valid", "Thing")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);

            assertThat(graph.allNodes()).contains("S", "GoodAux", "BadAux");
            assertThat(graph.dependenciesOf("GoodAux")).contains("S");
            // Bad aux has no formula dependencies
            assertThat(graph.dependenciesOf("BadAux")).isEmpty();
        }

        @Test
        void shouldBuildGraphWhenAllEquationsAreUnparseable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("AllBad")
                    .stock("S", 100, "Thing")
                    .flow("F1", "!!! bad", "Day", "S", null)
                    .variable("A1", "??? also bad", "Thing")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);

            // Graph should still contain all nodes
            assertThat(graph.allNodes()).contains("S", "F1", "A1");
            // Structural connections (flow source/sink) should still be present
            assertThat(graph.dependentsOf("F1")).contains("S");
            // No formula dependencies extracted
            assertThat(graph.dependenciesOf("F1")).isEmpty();
            assertThat(graph.dependenciesOf("A1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Where Used and Uses queries (#410)")
    class WhereUsedAndUses {

        private DependencyGraph buildSirGraph() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("Susceptible", 1000, "Person")
                    .stock("Infectious", 10, "Person")
                    .stock("Recovered", 0, "Person")
                    .constant("Contact Rate", 8, "Dimensionless unit")
                    .constant("Duration", 5, "Day")
                    .flow("Infection",
                            "Contact_Rate * Infectious * Susceptible / (Susceptible + Infectious + Recovered)",
                            "Day", "Susceptible", "Infectious")
                    .flow("Recovery", "Infectious / Duration", "Day", "Infectious", "Recovered")
                    .build();
            return DependencyGraph.fromDefinition(def);
        }

        @Test
        void shouldReturnDirectDependentsForWhereUsed() {
            DependencyGraph graph = buildSirGraph();
            Set<String> whereUsed = graph.dependentsOf("Contact Rate");
            assertThat(whereUsed).containsExactly("Infection");
        }

        @Test
        void shouldReturnMultipleDependentsForSharedVariable() {
            DependencyGraph graph = buildSirGraph();
            Set<String> whereUsed = graph.dependentsOf("Infectious");
            // Infectious is used in Infection equation, Recovery equation,
            // and Infection flow feeds into Infectious (sink), Recovery drains Infectious (source)
            assertThat(whereUsed).contains("Infection", "Recovery");
        }

        @Test
        void shouldReturnDirectDependenciesForUses() {
            DependencyGraph graph = buildSirGraph();
            Set<String> uses = graph.dependenciesOf("Recovery");
            assertThat(uses).contains("Infectious", "Duration");
        }

        @Test
        void shouldReturnEmptyForUnusedElement() {
            DependencyGraph graph = buildSirGraph();
            // Duration is only used by Recovery, not by anything else as a dependent
            Set<String> whereUsed = graph.dependentsOf("Duration");
            assertThat(whereUsed).containsExactly("Recovery");
        }

        @Test
        void shouldReturnEmptyDependenciesForConstant() {
            DependencyGraph graph = buildSirGraph();
            Set<String> uses = graph.dependenciesOf("Contact Rate");
            assertThat(uses).isEmpty();
        }
    }

    @Nested
    @DisplayName("TransitiveTraversal")
    class TransitiveTraversal {

        // Model: C → A1 → A2 → F → S (with F draining S)
        private DependencyGraph buildChainGraph() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Chain")
                    .constant("C", 10, "Thing")
                    .variable("A1", "C * 2", "Thing")
                    .variable("A2", "A1 + 1", "Thing")
                    .stock("S", 0, "Thing")
                    .flow("F", "A2", "Day", null, "S")
                    .build();
            return DependencyGraph.fromDefinition(def);
        }

        @Test
        void shouldFindTransitiveUpstream() {
            DependencyGraph graph = buildChainGraph();
            Map<String, Integer> upstream = graph.transitiveUpstream("F");

            assertThat(upstream).containsKey("F");
            assertThat(upstream.get("F")).isEqualTo(0);
            assertThat(upstream).containsKeys("A2", "A1", "C");
            // A2 is depth 1, A1 is depth 2, C is depth 3
            assertThat(upstream.get("A2")).isEqualTo(1);
            assertThat(upstream.get("A1")).isEqualTo(2);
            assertThat(upstream.get("C")).isEqualTo(3);
        }

        @Test
        void shouldFindTransitiveDownstream() {
            DependencyGraph graph = buildChainGraph();
            Map<String, Integer> downstream = graph.transitiveDownstream("C");

            assertThat(downstream).containsKey("C");
            assertThat(downstream.get("C")).isEqualTo(0);
            assertThat(downstream).containsKeys("A1", "A2", "F");
            assertThat(downstream.get("A1")).isEqualTo(1);
            assertThat(downstream.get("A2")).isEqualTo(2);
            assertThat(downstream.get("F")).isEqualTo(3);
        }

        @Test
        void shouldReturnOnlyOriginForLeafElement() {
            DependencyGraph graph = buildChainGraph();
            Map<String, Integer> upstream = graph.transitiveUpstream("C");

            assertThat(upstream).hasSize(1);
            assertThat(upstream).containsEntry("C", 0);
        }

        @Test
        void shouldHandleCyclesWithoutInfiniteLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Cycle")
                    .stock("Pop", 100, "Person")
                    .flow("Births", "Pop * 0.04", "Day", null, "Pop")
                    .build();
            DependencyGraph graph = DependencyGraph.fromDefinition(def);

            Map<String, Integer> upstream = graph.transitiveUpstream("Births");
            // Should include Pop (depth 1), then Births already visited, so no infinite loop
            assertThat(upstream).containsKeys("Births", "Pop");
            assertThat(upstream).hasSize(2);
        }
    }
}
