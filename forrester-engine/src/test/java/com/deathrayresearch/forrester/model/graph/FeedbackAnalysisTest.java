package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedbackAnalysis")
class FeedbackAnalysisTest {

    @Nested
    @DisplayName("simple feedback loop")
    class SimpleFeedbackLoop {

        @Test
        void shouldDetectLoopParticipants() {
            // Population → Births (formula) and Births → Population (flow sink)
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Population Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.loopParticipants())
                    .containsExactlyInAnyOrder("Population", "Births");
            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldDetectLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Population Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.isLoopEdge("Population", "Births")).isTrue();
            assertThat(analysis.isLoopEdge("Births", "Population")).isTrue();
        }

        @Test
        void shouldReportIsInLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Population Growth")
                    .stock("Population", 100, "Person")
                    .constant("Rate", 0.04, "Dimensionless unit")
                    .flow("Births", "Population * Rate", "Day", null, "Population")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.isInLoop("Population")).isTrue();
            assertThat(analysis.isInLoop("Births")).isTrue();
            assertThat(analysis.isInLoop("Rate")).isFalse();
        }
    }

    @Nested
    @DisplayName("no loops")
    class NoLoops {

        @Test
        void shouldReturnEmptyForAcyclicModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Acyclic")
                    .constant("Rate", 5, "Thing")
                    .stock("S", 0, "Thing")
                    .flow("F", "Rate", "Day", null, "S")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopGroups()).isEmpty();
            assertThat(analysis.loopEdges()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldHandleEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("multiple independent loops")
    class MultipleLoops {

        @Test
        void shouldDetectTwoIndependentLoops() {
            // Loop 1: Population → Births → Population
            // Loop 2: Tank → Drain → Tank
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .stock("Tank", 200, "Litre")
                    .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.loopParticipants())
                    .containsExactlyInAnyOrder("Population", "Births", "Tank", "Drain");
            assertThat(analysis.loopCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("shared-node loops")
    class SharedNodeLoops {

        @Test
        void shouldGroupSharedNodeLoopsAsSingleGroup() {
            // Stock feeds two flows, both flow back into it
            // Population → Births → Population and Population → Deaths → Population
            // These share "Population" so they should be in one group
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Shared Node")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .flow("Deaths", "Population * 0.02", "Day", "Population", null)
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.loopParticipants())
                    .contains("Population", "Births", "Deaths");
            // All connected → single group
            assertThat(analysis.loopCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("SIR model")
    class SirModel {

        @Test
        void shouldDetectLoopsInSirModel() {
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
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            // Infection, Susceptible, Infectious, Recovery, Recovered are all in loops
            // (the stock-flow connections create cycles)
            assertThat(analysis.loopParticipants())
                    .contains("Infectious", "Infection");

            // Constants should NOT be in loops — they don't receive feedback
            assertThat(analysis.isInLoop("Contact Rate")).isFalse();
            assertThat(analysis.isInLoop("Infectivity")).isFalse();
            assertThat(analysis.isInLoop("Duration")).isFalse();

            assertThat(analysis.loopCount()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("drain loop (stock as source)")
    class DrainLoop {

        @Test
        void shouldDetectDrainLoop() {
            // Tank → Drain (formula dep: Drain = Tank * Rate)
            // Drain → Tank (flow source connection)
            // This forms a feedback loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Drain")
                    .stock("Tank", 100, "Litre")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                    .build();

            DependencyGraph graph = DependencyGraph.fromDefinition(def);
            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(graph);

            assertThat(analysis.isInLoop("Tank")).isTrue();
            assertThat(analysis.isInLoop("Drain")).isTrue();
            assertThat(analysis.isInLoop("Rate")).isFalse();
            assertThat(analysis.loopCount()).isEqualTo(1);
        }
    }
}
