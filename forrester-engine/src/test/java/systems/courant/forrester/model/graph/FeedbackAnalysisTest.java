package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedbackAnalysis")
class FeedbackAnalysisTest {

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

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

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

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagLinearChain() {
            // A → B → C with each flow referencing only its source stock
            // This is the user's SIR-like model — no feedback
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Linear SIR")
                    .stock("A", 100, "Person")
                    .stock("B", 10, "Person")
                    .stock("C", 0, "Person")
                    .flow("AB", "A * 0.1", "Day", "A", "B")
                    .flow("BC", "B * 0.05", "Day", "B", "C")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("single-stock loops (not detected)")
    class SingleStockLoops {

        @Test
        void shouldNotFlagPopulationGrowthLoop() {
            // Population → Births → Population is a single-stock self-influence.
            // Not a multi-stock feedback loop.
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Population Growth")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagDrainLoop() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Drain")
                    .stock("Tank", 100, "Litre")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .flow("Drain", "Tank * Rate", "Day", "Tank", null)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("Tank")).isFalse();
            assertThat(analysis.isInLoop("Drain")).isFalse();
            assertThat(analysis.isInLoop("Rate")).isFalse();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldNotFlagSharedStockWithTwoFlows() {
            // Population with Births and Deaths — both reference Population
            // but it's still a single-stock structure, not a multi-stock loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Shared Node")
                    .stock("Population", 100, "Person")
                    .flow("Births", "Population * 0.04", "Day", null, "Population")
                    .flow("Deaths", "Population * 0.02", "Day", "Population", null)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.loopParticipants()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("multi-stock feedback loops")
    class MultiStockLoops {

        @Test
        void shouldDetectPredatorPreyLoop() {
            // Predation flow references both Prey and Predators, creating
            // a Prey ↔ Predators feedback loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("Prey")).isTrue();
            assertThat(analysis.isInLoop("Predators")).isTrue();
            assertThat(analysis.isInLoop("Predation")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldDetectLoopsInSirModel() {
            // In the full SIR model, Infection depends on Susceptible, Infectious,
            // and Recovered — creating a multi-stock feedback structure
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
                    .flow("Recovery", "Infectious / Duration", "Day",
                            "Infectious", "Recovered")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(sir);

            // All three stocks are in the loop (Recovered appears in Infection's
            // denominator, so it causally influences other stocks via the flow)
            assertThat(analysis.isInLoop("Susceptible")).isTrue();
            assertThat(analysis.isInLoop("Infectious")).isTrue();
            assertThat(analysis.isInLoop("Recovered")).isTrue();

            // Both flows participate
            assertThat(analysis.isInLoop("Infection")).isTrue();
            assertThat(analysis.isInLoop("Recovery")).isTrue();

            // Constants do NOT participate
            assertThat(analysis.isInLoop("Contact Rate")).isFalse();
            assertThat(analysis.isInLoop("Infectivity")).isFalse();
            assertThat(analysis.isInLoop("Duration")).isFalse();

            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldDetectTwoIndependentLoops() {
            // Two separate predator-prey-like loops
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two Loops")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .stock("C", 200, "Thing")
                    .stock("D", 30, "Thing")
                    .flow("F1", "A * B * 0.01", "Day", "A", "B")
                    .flow("F2", "C * D * 0.02", "Day", "C", "D")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("A")).isTrue();
            assertThat(analysis.isInLoop("B")).isTrue();
            assertThat(analysis.isInLoop("C")).isTrue();
            assertThat(analysis.isInLoop("D")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(2);
        }

        @Test
        void shouldDetectLoopThroughAuxiliary() {
            // A → G → B and B → (via aux X) → F → A
            // Tests transitive dependency resolution through auxiliaries
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Aux Loop")
                    .stock("A", 100, "Thing")
                    .stock("B", 50, "Thing")
                    .aux("X", "B * 2", "Thing")
                    .flow("F", "X", "Day", "B", "A")
                    .flow("G", "A * 0.1", "Day", "A", "B")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("A")).isTrue();
            assertThat(analysis.isInLoop("B")).isTrue();
            assertThat(analysis.isInLoop("F")).isTrue();
            assertThat(analysis.isInLoop("G")).isTrue();
            assertThat(analysis.loopCount()).isEqualTo(1);
        }

        @Test
        void shouldReportLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Predator-Prey")
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Formula dependency edges: stocks → flow
            assertThat(analysis.isLoopEdge("Prey", "Predation")).isTrue();
            assertThat(analysis.isLoopEdge("Predators", "Predation")).isTrue();

            // Material flow edges: flow → stocks
            assertThat(analysis.isLoopEdge("Predation", "Prey")).isTrue();
            assertThat(analysis.isLoopEdge("Predation", "Predators")).isTrue();
        }
    }
}
