package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.graph.FeedbackAnalysis.CausalLoop;
import systems.courant.forrester.model.graph.FeedbackAnalysis.LoopType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CLD Loop Detection")
class CldLoopDetectionTest {

    @Nested
    @DisplayName("no CLD loops")
    class NoLoops {

        @Test
        void shouldReturnEmptyForModelWithoutCausalLinks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No links")
                    .cldVariable("A")
                    .cldVariable("B")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldReturnEmptyForAcyclicCld() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Acyclic")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }

        @Test
        void shouldReturnEmptyForEmptyModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).isEmpty();
            assertThat(analysis.loopCount()).isZero();
        }
    }

    @Nested
    @DisplayName("reinforcing loops")
    class ReinforcingLoops {

        @Test
        void shouldDetectSimpleReinforcingLoop() {
            // A →(+) B →(+) A: zero negatives = reinforcing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("R loop")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.REINFORCING);
            assertThat(loop.label()).isEqualTo("R1");
            assertThat(loop.path()).containsExactly("A", "B");
        }

        @Test
        void shouldClassifyDoubleNegativeAsReinforcing() {
            // A →(-) B →(-) A: two negatives = reinforcing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Double negative")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            assertThat(analysis.causalLoops().getFirst().type())
                    .isEqualTo(LoopType.REINFORCING);
        }

        @Test
        void shouldDetectThreeNodeReinforcingLoop() {
            // A →(+) B →(+) C →(+) A: zero negatives
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Three-node R")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.REINFORCING);
            assertThat(loop.path()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("balancing loops")
    class BalancingLoops {

        @Test
        void shouldDetectSimpleBalancingLoop() {
            // A →(+) B →(-) A: one negative = balancing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("B loop")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.BALANCING);
            assertThat(loop.label()).isEqualTo("B1");
        }

        @Test
        void shouldClassifyThreeNegativesAsBalancing() {
            // A →(-) B →(-) C →(-) A: three negatives (odd) = balancing
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Triple negative")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            assertThat(analysis.causalLoops().getFirst().type())
                    .isEqualTo(LoopType.BALANCING);
        }
    }

    @Nested
    @DisplayName("indeterminate loops")
    class IndeterminateLoops {

        @Test
        void shouldClassifyLoopWithUnknownPolarityAsIndeterminate() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unknown polarity")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.UNKNOWN)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(1);
            CausalLoop loop = analysis.causalLoops().getFirst();
            assertThat(loop.type()).isEqualTo(LoopType.INDETERMINATE);
            assertThat(loop.label()).isEqualTo("?1");
        }
    }

    @Nested
    @DisplayName("multiple loops")
    class MultipleLoops {

        @Test
        void shouldDetectTwoIndependentLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Two loops")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "D", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "C", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.causalLoops()).hasSize(2);
            assertThat(analysis.loopCount()).isEqualTo(2);

            long reinforcing = analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.REINFORCING).count();
            long balancing = analysis.causalLoops().stream()
                    .filter(l -> l.type() == LoopType.BALANCING).count();
            assertThat(reinforcing).isEqualTo(1);
            assertThat(balancing).isEqualTo(1);
        }

        @Test
        void shouldDetectMultipleCyclesInSameComponent() {
            // A ↔ B ↔ C with A→C and C→A creates multiple cycles:
            // A→B→A, B→C→B, A→B→C→A
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Overlapping")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "B", CausalLinkDef.Polarity.NEGATIVE)
                    .causalLink("A", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // Should find at least the 3 cycles listed above
            assertThat(analysis.causalLoops().size()).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("loop participants and edges")
    class ParticipantsAndEdges {

        @Test
        void shouldIncludeCldVariablesInLoopParticipants() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Participants")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .cldVariable("Z")
                    .causalLink("X", "Y", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Y", "X", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isInLoop("X")).isTrue();
            assertThat(analysis.isInLoop("Y")).isTrue();
            assertThat(analysis.isInLoop("Z")).isFalse();
        }

        @Test
        void shouldIncludeCausalLinkEdgesInLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Edges")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isLoopEdge("A", "B")).isTrue();
            assertThat(analysis.isLoopEdge("B", "A")).isTrue();
        }

        @Test
        void shouldNotIncludeNonLoopEdges() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Non-loop edge")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "C", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            assertThat(analysis.isLoopEdge("A", "B")).isTrue();
            assertThat(analysis.isLoopEdge("B", "A")).isTrue();
            assertThat(analysis.isLoopEdge("B", "C")).isFalse();
        }
    }

    @Nested
    @DisplayName("mixed SF and CLD models")
    class MixedModels {

        @Test
        void shouldDetectBothSfAndCldLoops() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed")
                    // SF feedback loop
                    .stock("Prey", 100, "Animal")
                    .stock("Predators", 20, "Animal")
                    .flow("Predation", "Prey * Predators * 0.01", "Day",
                            "Prey", "Predators")
                    // CLD feedback loop
                    .cldVariable("Stress")
                    .cldVariable("Burnout")
                    .causalLink("Stress", "Burnout", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("Burnout", "Stress", CausalLinkDef.Polarity.POSITIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            // SF loop
            assertThat(analysis.isInLoop("Prey")).isTrue();
            assertThat(analysis.isInLoop("Predators")).isTrue();
            assertThat(analysis.loopGroups()).hasSize(1);

            // CLD loop
            assertThat(analysis.isInLoop("Stress")).isTrue();
            assertThat(analysis.isInLoop("Burnout")).isTrue();
            assertThat(analysis.causalLoops()).hasSize(1);
            assertThat(analysis.causalLoops().getFirst().type())
                    .isEqualTo(LoopType.REINFORCING);

            // Total count includes both
            assertThat(analysis.loopCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("loop labels")
    class LoopLabels {

        @Test
        void shouldNumberLoopsByType() {
            // Create two reinforcing and one balancing loop
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Labels")
                    .cldVariable("A")
                    .cldVariable("B")
                    .cldVariable("C")
                    .cldVariable("D")
                    .cldVariable("E")
                    .cldVariable("F")
                    .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("B", "A", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("C", "D", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("D", "C", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("E", "F", CausalLinkDef.Polarity.POSITIVE)
                    .causalLink("F", "E", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);

            List<String> labels = analysis.causalLoops().stream()
                    .map(CausalLoop::label).toList();
            assertThat(labels).contains("R1", "R2", "B1");
        }
    }
}
